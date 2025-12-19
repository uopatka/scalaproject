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
    val bookEntries = bookEntryRepository.findAll()
    val books = bookRepository.findAll()
    Ok(views.html.index(bookEntries, books)) // selectedBook = None by default
    // Q(AW): should I pass selectedBook = none explicitly?
  }

  def showBook(isbn: String) = Action { implicit request =>
    val bookEntries = bookEntryRepository.findAll()
    val books = bookRepository.findAll()
    val selectedBook: Option[(BookEntry, Book)] =
      for {
        entry <- bookEntries.find(_.isbn == isbn)
        book  <- books.find(_.isbn == isbn)
    } yield (entry, book)

    Ok(views.html.index(bookEntries, books, selectedBook))
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

      isbn =>
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
    )
  }

}
