package controllers

import models.{Book, Entry, User, BookStatus, Note, Publication, DisplayItem}

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
import persistence.PublicationRepository
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
                                publicationRepository: PublicationRepository,
                                bookEntryRepository: BookEntryRepository,
                                userRepository: persistence.UserRepository,
                                openLibraryService: services.OpenLibraryService,
                                crossrefService: services.CrossrefService,
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

    val entriesF: Future[List[Entry]] = maybeUserId match {
      case Some(userId) => bookEntryRepository.getAll().map(_.toList.filter(_.userId == userId))
      case None         => bookEntryRepository.getAll().map(_.toList.filter(_.userId == 0L))
    }

    for {
      entries <- entriesF
      books <- bookRepository.getAll()
      publications <- publicationRepository.getAll()
    } yield {
      val allItems: Seq[DisplayItem] = books.map(b => b: DisplayItem) ++ publications.map(p => p: DisplayItem)
      
      val (sortedEntries, sortedItems) =
        utils.BookUtils.filterAndSortItems(entries, allItems, filters)

      val notes: Seq[Note] = Seq.empty

      Ok(views.html.index(sortedEntries, sortedItems, selectedBook = None, notes, maybeUsername, filters))
    }
  }

  def showBookByEntryId(entryId: Long) = Action.async { implicit request =>
    bookEntryRepository.getById(entryId).flatMap {
      case Some(entry) => showBook(entry.refId).apply(request)
      case None => Future.successful(NotFound("Nie znaleziono książki"))
    }
  }

def showBook(posId: String) = Action.async { implicit request =>
  val maybeUsername: Option[String] = request.session.get("username")
  val maybeUserId: Option[Long] = request.session.get("userId").map(_.toLong)

  val filters: Map[String, String] =
    request.queryString.view.mapValues(_.head).toMap

  val entriesF: Future[List[Entry]] = maybeUserId match {
    case Some(userId) => bookEntryRepository.getAll().map(_.toList.filter(_.userId == userId))
    case None         => bookEntryRepository.getAll().map(_.toList.filter(_.userId == 0L))
  }

  entriesF.flatMap { entries =>
    bookRepository.getAll().flatMap { booksSeq =>
      publicationRepository.getAll().flatMap { publicationsSeq =>
        val allItems: Seq[DisplayItem] =
          booksSeq.map(b => b: DisplayItem) ++ publicationsSeq.map(p => p: DisplayItem)

        val (sortedEntries, sortedItems) =
          utils.BookUtils.filterAndSortItems(entries, allItems, filters)

        val selectedItem: Option[(Entry, DisplayItem)] = for {
          entry <- sortedEntries.find(_.refId == posId)
          item  <- sortedItems.find(_.id == posId)
        } yield (entry, item)

        val notesF: Future[Seq[Note]] = selectedItem match {
          case Some((entry, _)) => noteRepository.findByBookEntry(entry.id)
          case None             => Future.successful(Seq.empty)
        }

        notesF.map { notes =>
          Ok(views.html.index(sortedEntries, sortedItems, selectedItem, notes, maybeUsername, filters))
        }
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

  val publicationForm: Form[String] = Form(
    single(
      "doi" -> nonEmptyText.verifying(
        "Nieprawidłowy DOI",
        doi => doi.matches("""10\.\d{4,9}/[-._;()/:A-Z0-9]+""") || doi.matches("""10\.\d{4,9}/[-._;()/:A-Z0-9]+""")
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
                val ensureGuestF: Future[Unit] = if (userId == 0L) {
                  userRepository.getById(0L).flatMap {
                    case Some(_) => Future.successful(())
                    case None    => userRepository.insert(User(0L, "guest", "")).map(_ => ())
                  }
                } else Future.successful(())

                for {
                  existsOpt <- bookRepository.getByIsbn(fetchedBook.id)
                  _ <- existsOpt match {
                    case Some(_) => Future.successful(0)
                    case None    => bookRepository.insert(fetchedBook).map(_ => 1)
                  }
                  _ <- ensureGuestF
                  _ <- bookEntryRepository.insert(Entry(id = 0L,
                    userId = userId,
                    entryType = models.EntryType.Book,
                    refId = fetchedBook.id,
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

  def addPublication() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.addPublication(publicationForm))
  }

  def addPublicationSubmit() = Action.async { implicit request =>
    val maybeUserId: Option[Long] =
      request.session.get("userId").map(_.toLong)

    val userId: Long = maybeUserId.getOrElse(0L) // guest = 0

    publicationForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(
          BadRequest(views.html.addPublication(formWithErrors))
        ),

      doi => {
        bookEntryRepository.getAll().map(_.exists(entry =>
          entry.userId == userId && entry.refId == doi
        )).flatMap { alreadyExists =>
          if (alreadyExists) {
            val boundForm = publicationForm.bindFromRequest()
            Future.successful(
              BadRequest(
                views.html.addPublication(
                  boundForm.withGlobalError("Ta publikacja jest już w Twojej bibliotece")
                )
              )
            )
          } else {
            crossrefService.fetchByDoi(doi).flatMap {
              case Some(fetchedPublication) =>
                // ensureCover?
                val ensureGuestF: Future[Unit] = if (userId == 0L) {
                  userRepository.getById(0L).flatMap {
                    case Some(_) => Future.successful(())
                    case None    => userRepository.insert(User(0L, "guest", "")).map(_ => ())
                  }
                } else Future.successful(())

                for {
                  existsOpt <- publicationRepository.getByDoi(fetchedPublication.doi)
                  _ <- existsOpt match {
                    case Some(_) => Future.successful(0) // already in database -> do nothing
                    case None    => publicationRepository.insert(fetchedPublication).map(_ => 1)
                  }
                  _ <- ensureGuestF
                  _ <- bookEntryRepository.insert(Entry(id = 0L,
                    userId = userId,
                    entryType = models.EntryType.Publication,
                    refId = fetchedPublication.doi,
                    altCover = ""))
                } yield Redirect(routes.HomeController.index())

              case None =>
                val boundForm = publicationForm.bindFromRequest()
                Future.successful(
                  BadRequest(
                    views.html.addPublication(
                      boundForm.withGlobalError("Nie znaleziono publikacji dla tego DOI")
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

  def createPublication(doi: String) = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.addPublication(publicationForm))
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
      itemOpt <- entryOpt match {
        case Some(entry) => 
          bookRepository.getByIsbn(entry.refId).flatMap {
            case Some(book) => Future.successful(Some(book: DisplayItem))
            case None       => publicationRepository.getByDoi(entry.refId).map(_.map(p => p: DisplayItem))
          }
        case None => Future.successful(None)
      }
    } yield {
      (entryOpt, itemOpt) match {
        case (Some(entry), Some(item)) => Ok(views.html.editBookEntry(entry, ensureCover(item), maybeUsername))
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
                val ensureGuestF: Future[Unit] = if (userId == 0L) {
                  userRepository.getById(0L).flatMap {
                    case Some(_) => Future.successful(())
                    case None    => userRepository.insert(User(0L, "guest", "")).map(_ => ())
                  }
                } else Future.successful(())

                for {
                  existsOpt <- bookRepository.getByIsbn(fetchedBook.id)
                  _ <- existsOpt match {
                    case Some(_) => Future.successful(0) // already in database -> do nothing
                    case None    => bookRepository.insert(fetchedBook).map(_ => 1)
                  }
                  _ <- ensureGuestF
                  _ <- bookEntryRepository.insert(Entry(0L, userId, models.EntryType.Book, fetchedBook.id))
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

  private def ensureCover(item: DisplayItem): DisplayItem = {
    val coverValue = if (item.cover.isEmpty) "/assets/images/placeholder_cover.png" else item.cover
    item match {
      case book: Book => book.copy(cover = coverValue)
      case publication: Publication => publication.copy(cover = coverValue)
    }
  }

  def serveUpload(filename: String) = Action {
    val uploadDir = Paths.get(config.get[String]("app.uploads.dir"))
    val file = uploadDir.resolve(filename).toFile

    if (file.exists()) Ok.sendFile(file)
    else NotFound("Nie znaleziono wpisu")
  }

  def drawToReadBook = Action.async { implicit request =>
    request.session.get("userId") match {
      case Some(id) =>
        val uid = id.toLong
        // Pobierz wszystkie książki użytkownika ze statusem "Do przeczytania"
        bookEntryRepository.findByUserAndStatus(uid, BookStatus.ToRead).flatMap { entries =>
          if (entries.isEmpty) {
            // brak książek → None
            Future.successful(Ok(views.html.drawBook(None)))
          } else {
            // losujemy jedną książkę
            val randomEntry = entries(scala.util.Random.nextInt(entries.length))

            randomEntry.entryType match {
            case models.EntryType.Book =>
              bookRepository.getByIsbn(randomEntry.refId).map { maybeBook =>
                Ok(views.html.drawBook(Some((randomEntry, maybeBook))))
              }

            case models.EntryType.Publication =>
              publicationRepository.getByDoi(randomEntry.refId).map { maybePub =>
                Ok(views.html.drawBook(Some((randomEntry, maybePub))))
              }
            }
          }
        }

      case None =>
        Future.successful(Redirect(routes.AuthController.loginPage()))
    }
  }



}
