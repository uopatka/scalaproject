package controllers

import models.{Book, BookEntry, User, BookStatus, Note}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, single}
import play.api.i18n.I18nSupport
import play.api.mvc._
import java.nio.file.Paths
import java.nio.file.Files
import play.api.Configuration
import javax.inject._

import repositories.{BookRepository => TestBookRepository}
import repositories.{BookEntryRepository => TestBookEntryRepository}
import forms.CreateBookForm
import persistence.BookRepository
import persistence.BookEntryRepository
import persistence.NoteRepository
import persistence.SlickColumnMappers._
import play.api.i18n.Messages

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                val controllerComponents: ControllerComponents,
                                bookRepository: BookRepository,
                                bookEntryRepository: BookEntryRepository,
                                userRepository: persistence.UserRepository,
                                openLibraryService: services.OpenLibraryService,
                                noteRepository: NoteRepository,
                                config: Configuration
                              )(implicit ec: ExecutionContext)
                                extends BaseController with I18nSupport {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action.async { implicit request =>
    val maybeUsername: Option[String] = request.session.get("username")
    val maybeUserId: Option[Long] = request.session.get("userId").map(_.toLong)

    val bookEntriesF: Future[List[BookEntry]] = maybeUserId match {
      case Some(userId) =>
        bookEntryRepository.getAll().map(_.toList.filter(_.userId == userId))
      case None =>
        bookEntryRepository.getAll().map(_.toList.filter(_.userId == 0L))
    }

    for {
      bookEntries <- bookEntriesF
      booksSeq    <- bookRepository.getAll()
    } yield {
      val userIsbns = bookEntries.map(_.isbn).toSet
      val books = booksSeq.filter(b => userIsbns.contains(b.isbn)).map(ensureCover).toList
      val notes: Seq[Note] = Seq.empty
      Ok(views.html.index(bookEntries, books, None, notes, maybeUsername))
    }
  }

  def showBookByEntryId(entryId: Long) = Action.async { implicit request =>
    bookEntryRepository.getById(entryId).flatMap {
      case Some(entry) => showBook(entry.isbn).apply(request)
      case None => Future.successful(NotFound("Nie znaleziono książki"))
    }
  }

  def showBook(isbn: String) = Action.async { implicit request =>
    val maybeUsername: Option[String] = request.session.get("username")
    val maybeUserId: Option[Long] = request.session.get("userId").map(_.toLong)

    val bookEntriesF: Future[List[BookEntry]] = maybeUserId match {
      case Some(userId) =>
        bookEntryRepository.getAll().map(_.toList.filter(_.userId == userId))
      case None =>
        bookEntryRepository.getAll().map(_.toList.filter(_.userId == 0L))
    }

    val userIsbnsF: Future[Set[String]] = bookEntriesF.map(_.map(_.isbn).toSet)

    for {
      bookEntries <- bookEntriesF
      userIsbns  <- userIsbnsF
      booksSeq   <- bookRepository.getAll()
      books = booksSeq.toList.filter(b => userIsbns.contains(b.isbn)).map(ensureCover)
      
      selectedBook = for {
        entry <- bookEntries.find(_.isbn == isbn)
        book  <- books.find(_.isbn == isbn)
      } yield (entry, book)
      notes <- selectedBook match {
        case Some((entry, _)) => noteRepository.findByBookEntry(entry.id)
        case None => Future.successful(Seq.empty)
      }
    } yield {
      Ok(views.html.index(bookEntries, books, selectedBook, notes, maybeUsername))
    }
  }

  val bookForm: Form[String] = Form(
    single(
      "isbn" -> nonEmptyText.verifying(
        "Nieprawidłowy ISBN",
        isbn => isbn.replaceAll("-", "").matches("""\d{10}|\d{13}""")
      )
    )
  )

  def addBook() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.addBook(bookForm))
  }

  def addBookSubmit() = Action.async { implicit request =>
    val maybeUserId: Option[Long] =
      request.session.get("userId").map(_.toLong)

    val userId: Long = maybeUserId.getOrElse(0L) // guest = 0

    bookForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(
          BadRequest(views.html.addBook(formWithErrors))
        ),

      isbn => {
        bookEntryRepository.getAll().map(_.exists(entry =>
          entry.userId == userId && entry.isbn == isbn
        )).flatMap { alreadyExists =>
          if (alreadyExists) {
            val boundForm = bookForm.bindFromRequest()
            Future.successful(
              BadRequest(
                views.html.addBook(
                  boundForm.withGlobalError("Ta książka jest już w Twojej bibliotece")
                )
              )
            )
          } else {
            openLibraryService.fetchByIsbn(isbn).flatMap {
              case Some(fetchedBook) =>
                val bookWithCover = ensureCover(fetchedBook)
                val ensureGuestF: Future[Unit] = if (userId == 0L) {
                  userRepository.getById(0L).flatMap {
                    case Some(_) => Future.successful(())
                    case None    => userRepository.insert(User(0L, "guest", "")).map(_ => ())
                  }
                } else Future.successful(())

                for {
                  existsOpt <- bookRepository.getByIsbn(bookWithCover.isbn)
                  _ <- existsOpt match {
                    case Some(_) => Future.successful(0) // already in database -> do nothing
                    case None    => bookRepository.insert(bookWithCover).map(_ => 1)
                  }
                  _ <- ensureGuestF
                  _ <- bookEntryRepository.insert(BookEntry(id = 0L,
                    userId = userId,
                    isbn = bookWithCover.isbn,
                    altCover = ""))
                } yield Redirect(routes.HomeController.index())

              case None =>
                val boundForm = bookForm.bindFromRequest()
                Future.successful(
                  BadRequest(
                    views.html.addBook(
                      boundForm.withGlobalError("Nie znaleziono książki dla tego ISBN")
                    )
                  )
                )
            }
          }
        }
      }
    )
  }

  def createBook(isbn: String) = Action { implicit request: Request[AnyContent] =>
    val preFilledForm = CreateBookForm.form.fill(forms.BookData(
      isbn = isbn,
      title = "",
      author = "",
      publishYear = 0,
      pages = 0,
      cover = ""
    ))
    Ok(views.html.createBook(preFilledForm, isbn))
  }

  def createBookSubmit(isbn: String) = Action.async { implicit request =>
    val maybeUserId: Option[Long] = request.session.get("userId").map(_.toLong)
    val userId: Long = maybeUserId.getOrElse(0L)


    CreateBookForm.form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(
          BadRequest(
            views.html.createBook(
              formWithErrors.copy(data = formWithErrors.data + ("isbn" -> isbn)),
              isbn
            )
          )
        ),

      bookData => {
        val finalBookData = bookData.copy(isbn = isbn)


        val ensureGuestF: Future[Unit] = if (userId == 0L) {
          userRepository.getById(0L).flatMap {
            case Some(_) => Future.successful(())
            case None    => userRepository.insert(User(0L, "guest", "")).map(_ => ())
          }
        } else Future.successful(())

        ensureGuestF.flatMap { _ =>
          bookRepository.upsert(Book(
            isbn = finalBookData.isbn,
            title = finalBookData.title,
            author = finalBookData.author,
            publishYear = finalBookData.publishYear,
            pages = finalBookData.pages,
            cover = finalBookData.cover
          )).flatMap { _ =>
            bookEntryRepository.insert(BookEntry(
              id = 0L,
              userId = userId,
              isbn = finalBookData.isbn,
              altCover = ""
            ))
          }.map { _ =>
            Redirect(routes.HomeController.index())
          }
        }
      }
    )
  }


  def deleteBook(id: Long) = Action.async { implicit request =>
    bookEntryRepository.delete(id).map { _ =>
      Redirect(routes.HomeController.index())
    }
  }

  def editBookEntry(id: Long) = Action.async { implicit request =>
    val maybeUsername: Option[String] = request.session.get("username")
    for {
      entryOpt <- bookEntryRepository.getById(id)
      bookOpt <- entryOpt match {
        case Some(entry) => bookRepository.getByIsbn(entry.isbn)
        case None => Future.successful(None)
      }
    } yield {
      (entryOpt, bookOpt) match {
        case (Some(entry), Some(book)) => Ok(views.html.editBookEntry(entry, ensureCover(book), maybeUsername))
        case _ => NotFound("Książka nie znaleziona")
      }
    }
  }

  def editBookEntrySubmit() = Action.async { implicit request =>
    val maybeUserId: Option[Long] =
      request.session.get("userId").map(_.toLong)

    val userId: Long = maybeUserId.getOrElse(0L) // guest = 0

    bookForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(
          BadRequest(views.html.addBook(formWithErrors))
        ),

      isbn => {
        bookEntryRepository.getAll().map(_.exists(entry =>
          entry.userId == userId && entry.isbn == isbn
        )).flatMap { alreadyExists =>
          if (alreadyExists) {
            Future.successful(
              BadRequest(
                views.html.addBook(
                  bookForm.withGlobalError("Ta książka jest już w Twojej bibliotece")
                )
              )
            )
          } else {
            openLibraryService.fetchByIsbn(isbn).flatMap {
              case Some(fetchedBook) =>
                val bookWithCover = ensureCover(fetchedBook)
                val ensureGuestF: Future[Unit] = if (userId == 0L) {
                  userRepository.getById(0L).flatMap {
                    case Some(_) => Future.successful(())
                    case None    => userRepository.insert(User(0L, "guest", "")).map(_ => ())
                  }
                } else Future.successful(())

                for {
                  existsOpt <- bookRepository.getByIsbn(bookWithCover.isbn)
                  _ <- existsOpt match {
                    case Some(_) => Future.successful(0) // already in database -> do nothing
                    case None    => bookRepository.insert(bookWithCover).map(_ => 1)
                  }
                  _ <- ensureGuestF
                  _ <- bookEntryRepository.insert(BookEntry(0L, userId, bookWithCover.isbn))
                } yield Redirect(routes.HomeController.index())

              case None =>
                Future.successful(
                  BadRequest(
                    views.html.addBook(
                      bookForm.withGlobalError("Nie znaleziono książki dla tego ISBN")
                    )
                  )
                )
            }
          }
        }
      }
    )
  }

  def editBookCover(entryId: Long) = Action(parse.multipartFormData).async { implicit request =>
    request.body.file("cover") match {
      case Some(file) =>
        val filename = s"${java.time.Instant.now().toEpochMilli}_${file.filename}"
        val directory = Paths.get(config.get[String]("app.uploads.dir"))

        Files.createDirectories(directory)

        val path = directory.resolve(filename)

        file.ref.moveTo(path, replace = true)

        // Getting the entry id
        bookEntryRepository.getById(entryId).flatMap {
          case Some(entry) =>
            // Get book's ISBN
            val updated = entry.copy(
              altCover = filename
            )
            bookEntryRepository.update(updated).map { _ =>
              Redirect(routes.HomeController.editBookEntry(entryId))
            }
          case None =>
            Files.deleteIfExists(path)
            Future.successful(NotFound("Nie znaleziono wpisu"))
        }
      case None =>
        Future.successful(BadRequest("Nie przesłano pliku"))
    }

  }


  def updatePagesRead(id: Long) = Action { implicit request =>
    request.body.asFormUrlEncoded
      .flatMap(_.get("pagesRead").flatMap(_.headOption))
      .flatMap(s => scala.util.Try(s.toInt).toOption) match {

      case Some(pages) =>
        bookEntryRepository.updatePagesRead(id, pages)
        Redirect(routes.HomeController.editBookEntry(id))

      case None =>
        BadRequest("Nieprawidłowa liczba stron")
    }
  }

  def updateBookPages(entryId: Long) = Action.async { implicit request =>
    val pagesOpt = request.body.asFormUrlEncoded
      .flatMap(_.get("pages").flatMap(_.headOption))
      .flatMap(s => scala.util.Try(s.toInt).toOption)

    pagesOpt match {
      case Some(pages) if pages >= 0 =>
        for {
          entryOpt <- bookEntryRepository.getById(entryId)
          _ <- entryOpt match {
            case Some(entry) =>
              bookRepository.getByIsbn(entry.isbn).flatMap {
                case Some(book) =>
                  bookRepository.update(book.copy(pages = pages))
                case None => Future.successful(0)
              }
            case None => Future.successful(0)
          }
        } yield Redirect(routes.HomeController.editBookEntry(entryId))
      case _ => Future.successful(BadRequest("Nieprawidłowa liczba stron"))
    }
  }

  def updateBookYear(entryId: Long) = Action.async { implicit request =>
    val yearOpt = request.body.asFormUrlEncoded
      .flatMap(_.get("publishYear").flatMap(_.headOption))
      .flatMap(s => scala.util.Try(s.toInt).toOption)

    yearOpt match {
      case Some(year) if year >= 0 && year <= java.time.Year.now.getValue =>
        for {
          entryOpt <- bookEntryRepository.getById(entryId)
          _ <- entryOpt match {
            case Some(entry) =>
              bookRepository.getByIsbn(entry.isbn).flatMap {
                case Some(book) =>
                  bookRepository.update(book.copy(publishYear = year))
                case None => Future.successful(0)
              }
            case None => Future.successful(0)
          }
        } yield Redirect(routes.HomeController.editBookEntry(entryId))
      case _ => Future.successful(BadRequest("Nieprawidłowy rok"))
    }
  }

  def updateStatus(id: Long) = Action { implicit request =>
    val statusOpt: Option[BookStatus] =
      for {
        form <- request.body.asFormUrlEncoded
        value <- form.get("status").flatMap(_.headOption)
        status <- BookStatus.fromString(value.toLowerCase)
      } yield status

    statusOpt match {
      case Some(status) =>
        bookEntryRepository.updateStatus(id, status)
        Redirect(routes.HomeController.editBookEntry(id))

      case None =>
        BadRequest("Nieprawidłowy status")
    }
  }

  private def ensureCover(book: Book): Book = {
    val coverValue = if (book.cover.isEmpty) "/assets/images/placeholder_cover.png" else book.cover
    book.copy(cover = coverValue)
  }

  def serveUpload(filename: String) = Action {
    val uploadDir = Paths.get(config.get[String]("app.uploads.dir"))
    val file = uploadDir.resolve(filename).toFile

    if (file.exists()) Ok.sendFile(file)
    else NotFound("Nie znaleziono wpisu")
  }




}
