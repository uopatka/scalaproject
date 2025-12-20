package controllers

import models.{Book, BookEntry}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, single}
import play.api.i18n.I18nSupport
import play.api.mvc._

import javax.inject._
import repositories.BookRepository
import repositories.BookEntryRepository


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                val controllerComponents: ControllerComponents,
                                bookRepository: BookRepository,
                                bookEntryRepository: BookEntryRepository,
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
  def index() = Action { implicit request =>
    val maybeUsername: Option[String] = request.session.get("username")
    val maybeUserId: Option[Long] = request.session.get("userId").map(_.toLong)

    val bookEntries: List[BookEntry] = maybeUserId match {
      case Some(userId) => bookEntryRepository.findAll().filter(_.userId == userId)
      case None => List.empty // not logged in -> show empty list
    }

    val userIsbns: Set[String] = bookEntries.map(_.isbn).toSet
    val books: List[Book] = bookRepository.findAll().filter(book => userIsbns.contains(book.isbn))

    Ok(views.html.index(bookEntries, books, None, maybeUsername))
  }

  def showBook(isbn: String) = Action { implicit request =>
    val maybeUsername: Option[String] = request.session.get("username")
    val maybeUserId: Option[Long] = request.session.get("userId").map(_.toLong)

    val bookEntries: List[BookEntry] = maybeUserId match {
      case Some(userId) => bookEntryRepository.findAll().filter(_.userId == userId)
      case None => List.empty // not logged in -> show empty list
    }

    val userIsbns: Set[String] = bookEntries.map(_.isbn).toSet
    val books: List[Book] = bookRepository.findAll().filter(book => userIsbns.contains(book.isbn))

    val selectedBook: Option[(BookEntry, Book)] =
      for {
        entry <- bookEntries.find(_.isbn == isbn)
        book  <- books.find(_.isbn == isbn)
    } yield (entry, book)

    Ok(views.html.index(bookEntries, books, selectedBook, maybeUsername))
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
    bookForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(
          BadRequest(views.html.addBook(formWithErrors))
        ),

      isbn => {
        val userId: Long = 1 // TODO: replace with logged-in user ID

        val alreadyExists =
          bookEntryRepository.findAll().exists(entry =>
            entry.userId == userId && entry.isbn == isbn
          )

        if (alreadyExists) {
          Future.successful(
            BadRequest(
              views.html.addBook(
                bookForm.withGlobalError("Ta książka jest już w Twojej bibliotece")
              )
            )
          )
        } else {
          openLibraryService.fetchByIsbn(isbn).map {
            case Some(fetchedBook) =>
              bookRepository.add(fetchedBook)

              val userId: Long = 1
              val newEntry = BookEntry(
                id = bookEntryRepository.nextId(),
                userId = userId,
                isbn = fetchedBook.isbn
              )
              bookEntryRepository.add(newEntry)

              Redirect(routes.HomeController.index())

            case None =>
              BadRequest(
                views.html.addBook(
                  bookForm.withGlobalError("Nie znaleziono książki dla tego ISBN")
                )
              )
          }
        }
      }
    )
  }

}
