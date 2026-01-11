package controllers

import models.{Book, Entry, User, BookStatus, Note}

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
import java.time.LocalDate

import repositories.{BookRepository => TestBookRepository}
import repositories.{EntryRepository => TestEntryRepository}
import forms.CreateBookForm
import persistence.BookRepository
import persistence.BookEntryRepository
import persistence.NoteRepository
import persistence.SlickColumnMappers._
import play.api.i18n.Messages
import views.html.addBook.f
import models.Entry

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

    val filters: Map[String, String] =
      request.queryString.view.mapValues(_.head).toMap

    val statusFilter: Option[BookStatus] =
      filters.get("status").flatMap(s => BookStatus.fromString(s.toLowerCase))

    val searchQuery: Option[String] = filters.get("search").map(_.trim).filter(_.nonEmpty)

    val sortParam: String = filters.getOrElse("sort", "title_asc")

    val (sortField, sortOrder) = sortParam.split("_") match {
      case Array(field, order) => (field.toLowerCase, order.toLowerCase)
      case Array(field)        => (field.toLowerCase, "asc")
      case _                   => ("title", "asc")
    }

    val entriesF: Future[List[Entry]] = maybeUserId match {
      case Some(userId) =>
        bookEntryRepository.getAll().map(_.toList.filter(_.userId == userId))
      case None =>
        bookEntryRepository.getAll().map(_.toList.filter(_.userId == 0L))
    }

    for {
      entries <- entriesF
      booksSeq    <- bookRepository.getAll()
    } yield {
      val filteredEntries = statusFilter match {
        case Some(status) => entries.filter(_.status == status)
        case None         => entries
      }

      val userRefIds = filteredEntries.map(_.refId).toSet
      val filteredBooks =
        booksSeq.filter(b => userRefIds.contains(b.isbn)).map(ensureCover).toList
      
      val searchFilteredEntries = searchQuery match {
        case Some(query) =>
          val lowerCaseQuery = query.toLowerCase
          filteredEntries.filter { entry =>
            val matchingBook = filteredBooks.find(_.isbn == entry.refId)
            matchingBook.exists { book =>
              book.title.toLowerCase.contains(lowerCaseQuery) ||
              book.author.toLowerCase.contains(lowerCaseQuery)
              }
            }
        case None => filteredEntries
      } 

      val finalFilteredBooks = filteredBooks.filter { book =>
        searchFilteredEntries.exists(_.refId == book.isbn)
      }

      val sortedBooks = (sortField.toLowerCase, sortOrder.toLowerCase) match {
        case ("title", "asc")  => finalFilteredBooks.sortBy(_.title)
        case ("title", "desc") => finalFilteredBooks.sortBy(_.title)(Ordering[String].reverse)
        case ("author", "asc") => finalFilteredBooks.sortBy(_.author)
        case ("author", "desc") => finalFilteredBooks.sortBy(_.author)(Ordering[String].reverse)
        case ("year", "asc")   => finalFilteredBooks.sortBy(_.publishYear)
        case ("year", "desc")  => finalFilteredBooks.sortBy(_.publishYear)(Ordering[Int].reverse)
        case _                 => finalFilteredBooks
      }
      val sortedEntries = searchFilteredEntries.sortBy(entry => 
        sortedBooks.indexWhere(_.isbn == entry.refId)
      )

      val notes: Seq[Note] = Seq.empty

      Ok(views.html.index(sortedEntries, sortedBooks, selectedBook = None, notes, maybeUsername, filters))
    }
  }

  def showBookByEntryId(entryId: Long) = Action.async { implicit request =>
    bookEntryRepository.getById(entryId).flatMap {
      case Some(entry) => showBook(entry.refId).apply(request)
      case None => Future.successful(NotFound("Nie znaleziono książki"))
    }
  }

  def showBook(isbn: String) = Action.async { implicit request =>
    val maybeUsername: Option[String] = request.session.get("username")
    val maybeUserId: Option[Long] = request.session.get("userId").map(_.toLong)

    val filters: Map[String, String] = request.queryString.map { 
      case (k, v) => k -> v.head 
    }

    val statusFilter: Option[BookStatus] = request.getQueryString("status").flatMap(s => BookStatus.fromString(s.toLowerCase))

    val searchQuery: Option[String] = filters.get("search").map(_.trim).filter(_.nonEmpty)

    val sortParam: String = filters.getOrElse("sort", "title_asc")

    val (sortField, sortOrder) = sortParam.split("_") match {
      case Array(field, order) => (field.toLowerCase, order.toLowerCase)
      case Array(field)        => (field.toLowerCase, "asc")
      case _                   => ("title", "asc")
    }

    val entriesF: Future[List[Entry]] = maybeUserId match {
      case Some(userId) =>
        bookEntryRepository.getAll().map(_.toList.filter(_.userId == userId))
      case None =>
        bookEntryRepository.getAll().map(_.toList.filter(_.userId == 0L))
    }

    entriesF.flatMap { entries =>
      bookRepository.getAll().flatMap { booksSeq =>
        val filteredEntries = statusFilter match {
          case Some(status) => entries.filter(_.status == status)
          case None         => entries
        }

        val filteredBooks = booksSeq.filter(b => filteredEntries.map(_.refId).toSet.contains(b.isbn)).map(ensureCover).toList

        val searchFilteredEntries = searchQuery match {
          case Some(query) =>
            val lowerCaseQuery = query.toLowerCase
            filteredEntries.filter { entry =>
              val matchingBook = filteredBooks.find(_.isbn == entry.refId)
              matchingBook.exists { book =>
                book.title.toLowerCase.contains(lowerCaseQuery) ||
                book.author.toLowerCase.contains(lowerCaseQuery)
                }
              }
          case None => filteredEntries
        } 

        val finalFilteredBooks = filteredBooks.filter { book =>
          searchFilteredEntries.exists(_.refId == book.isbn)
        }

        val sortedBooks = (sortField.toLowerCase, sortOrder.toLowerCase) match {
          case ("title", "asc")  => finalFilteredBooks.sortBy(_.title)
          case ("title", "desc") => finalFilteredBooks.sortBy(_.title)(Ordering[String].reverse)
          case ("author", "asc") => finalFilteredBooks.sortBy(_.author)
          case ("author", "desc") => finalFilteredBooks.sortBy(_.author)(Ordering[String].reverse)
          case ("year", "asc")   => finalFilteredBooks.sortBy(_.publishYear)
          case ("year", "desc")  => finalFilteredBooks.sortBy(_.publishYear)(Ordering[Int].reverse)
          case _                 => finalFilteredBooks
        }

        val sortedEntries = searchFilteredEntries.sortBy(entry => 
          sortedBooks.indexWhere(_.isbn == entry.refId)
        )

        val selectedBook: Option[(Entry, Book)] = for {
          entry <- searchFilteredEntries.find(_.refId == isbn)
          book  <- sortedBooks.find(_.isbn == isbn)
        } yield (entry, book)

        val notesF: Future[Seq[Note]] = selectedBook match {
          case Some((entry, _)) => noteRepository.findByBookEntry(entry.id)
          case None             => Future.successful(Seq.empty)
        }

        notesF.map { notes =>
          Ok(views.html.index(sortedEntries, sortedBooks, selectedBook, notes, maybeUsername, filters))
        }
      }
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
          entry.userId == userId && entry.refId == isbn
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
                    case Some(_) => Future.successful(0)
                    case None    => bookRepository.insert(bookWithCover).map(_ => 1)
                  }
                  _ <- ensureGuestF
                  _ <- bookEntryRepository.insert(Entry(id = 0L,
                    userId = userId,
                    entryType = models.EntryType.Book,
                    refId = bookWithCover.isbn,
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
            bookEntryRepository.insert(Entry(
              id = 0L,
              userId = userId,
              entryType = models.EntryType.Book,
              refId = finalBookData.isbn,
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
        case Some(entry) => bookRepository.getByIsbn(entry.refId)
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
          entry.userId == userId && entry.refId == isbn
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
                  _ <- bookEntryRepository.insert(Entry(0L, userId, models.EntryType.Book, bookWithCover.isbn))
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
