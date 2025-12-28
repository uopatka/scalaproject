package controllers

import models.{Book, BookEntry, User, BookStatus}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, single}
import play.api.i18n.I18nSupport
import play.api.mvc._

import javax.inject._
import repositories.{BookRepository => TestBookRepository}
import repositories.{BookEntryRepository => TestBookEntryRepository}
import persistence.BookRepository
import persistence.BookEntryRepository
import persistence.SlickColumnMappers._


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
                                openLibraryService: services.OpenLibraryService
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
      val books = booksSeq.filter(b => userIsbns.contains(b.isbn)).toList
      Ok(views.html.index(bookEntries, books, None, maybeUsername))
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
    } yield {
      val books = booksSeq.toList.filter(b => userIsbns.contains(b.isbn))

      val selectedBook: Option[(BookEntry, Book)] =
        for {
          entry <- bookEntries.find(_.isbn == isbn)
          book  <- books.find(_.isbn == isbn)
        } yield (entry, book)

      Ok(views.html.index(bookEntries, books, selectedBook, maybeUsername))
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

    val userId: Long = maybeUserId.getOrElse(1L) // guest = 0

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
                val ensureGuestF: Future[Unit] = if (userId == 1L) {
                  userRepository.getById(0L).flatMap {
                    case Some(_) => Future.successful(())
                    case None    => userRepository.insert(User(0L, "guest", "")).map(_ => ())
                  }
                } else Future.successful(())

                for {
                  existsOpt <- bookRepository.getByIsbn(fetchedBook.isbn)
                  _ <- existsOpt match {
                    case Some(_) => Future.successful(0) // already in database -> do nothing
                    case None    => bookRepository.insert(fetchedBook).map(_ => 1)
                  }
                  _ <- ensureGuestF
                  _ <- bookEntryRepository.insert(BookEntry(0L, userId, fetchedBook.isbn))
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
        case (Some(entry), Some(book)) => Ok(views.html.editBookEntry(entry, book, maybeUsername))
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
                val ensureGuestF: Future[Unit] = if (userId == 0L) {
                  userRepository.getById(0L).flatMap {
                    case Some(_) => Future.successful(())
                    case None    => userRepository.insert(User(0L, "guest", "")).map(_ => ())
                  }
                } else Future.successful(())

                for {
                  existsOpt <- bookRepository.getByIsbn(fetchedBook.isbn)
                  _ <- existsOpt match {
                    case Some(_) => Future.successful(0) // already in database -> do nothing
                    case None    => bookRepository.insert(fetchedBook).map(_ => 1)
                  }
                  _ <- ensureGuestF
                  _ <- bookEntryRepository.insert(BookEntry(0L, userId, fetchedBook.isbn))
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
      case Some(coverFile) =>
        val filename = java.time.Instant.now().toEpochMilli + "_" + coverFile.filename
        val filePath = s"public/uploads/$filename"
        coverFile.ref.moveTo(new java.io.File(filePath), replace = true)

        // Pobierz entry z repozytorium
        bookEntryRepository.getById(entryId).flatMap {
          case Some(entry) =>
            // Pobierz książkę po ISBN
            bookRepository.getByIsbn(entry.isbn).flatMap {
              case Some(book) =>
                val updatedBook = book.copy(cover = s"/assets/uploads/$filename")
                bookRepository.update(updatedBook).map(_ =>
                  Redirect(routes.HomeController.editBookEntry(entryId))
                )
              case None =>
                Future.successful(NotFound("Nie znaleziono książki"))
            }
          case None =>
            Future.successful(NotFound("Nie znaleziono wpisu książki"))
        }

      case None =>
        Future.successful(BadRequest("Nie przesłano pliku okładki"))
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

}
