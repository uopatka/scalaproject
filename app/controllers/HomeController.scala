package controllers

import models.{Book, BookEntry}
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.i18n.I18nSupport
import play.api.mvc._

import javax.inject._
import repositories.BookRepository
import repositories.BookEntryRepository

import java.time.LocalDateTime

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
                                val controllerComponents: ControllerComponents,
                                bookRepository: BookRepository,
                                bookEntryRepository: BookEntryRepository,
                              ) extends BaseController with I18nSupport {

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

  val bookForm: Form[Book] = Form(
    mapping(
      "isbn" -> nonEmptyText,
      "title" -> nonEmptyText,
      "author" -> nonEmptyText
    )(
      (isbn, title, author) => Book(isbn, title, author, 0, 0, "to read")
    )(
      book => Some((book.isbn, book.title, book.author))
    )
  )


  def addBook() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.addBook(bookForm))
  }

  def addBookSubmit() = Action { implicit request =>
    bookForm.bindFromRequest().fold(
      formWithErrors => BadRequest(views.html.addBook(formWithErrors)),
      bookData => {
        bookRepository.add(bookData)

        val userId: Long = 1 // TODO: replace with logged-in user ID
        val newEntry = BookEntry(
          id = bookEntryRepository.nextId(),
          user_id = userId,
          isbn = bookData.isbn,
          created_at = LocalDateTime.now(),
          status = "to read",
          pages_read = 0
        )
        bookEntryRepository.add(newEntry)

        Redirect(routes.HomeController.index())
      }
    )
  }

}
