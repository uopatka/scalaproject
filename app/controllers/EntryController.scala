package controllers

import models.BookStatus

import play.api.i18n.I18nSupport
import play.api.mvc.BaseController
import scala.concurrent.ExecutionContext
import javax.inject._
import java.nio.file.Paths
import java.nio.file.Files
import play.api.mvc._
import play.api.Configuration
import persistence.BookRepository
import persistence.BookEntryRepository
import scala.concurrent.Future
import java.time.LocalDate

@Singleton
class EntryController @Inject()( 
                                val controllerComponents: ControllerComponents,
                                bookEntryRepository: BookEntryRepository,
                                bookRepository: BookRepository,
                                config: Configuration)
                                (implicit ec: ExecutionContext)
                                extends BaseController with I18nSupport{

    def editBookCover(entryId: Long) = Action(parse.multipartFormData).async { implicit request =>
      request.body.file("cover") match {
      case Some(file) =>
        val filename = s"${java.time.Instant.now().toEpochMilli}_${file.filename}"
        val directory = Paths.get(config.get[String]("app.uploads.dir"))

        Files.createDirectories(directory)

        val path = directory.resolve(filename)

        file.ref.moveTo(path, replace = true)

        bookEntryRepository.getById(entryId).flatMap {
          case Some(entry) =>
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
              bookRepository.getByIsbn(entry.refId).flatMap {
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
              bookRepository.getByIsbn(entry.refId).flatMap {
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

  def updateStatus(entryId: Long) = Action.async { implicit request =>
    val form = request.body.asFormUrlEncoded.getOrElse(Map.empty)

    val statusOpt: Option[BookStatus] =
      form.get("status").flatMap(_.headOption)
        .flatMap(s => BookStatus.fromString(s.toLowerCase()))

    //getting date of finishing reading if the user finished
    val finishedAtOpt: Option[LocalDate] =
      form.get("finishedAt")
        .flatMap(_.headOption)
        .filter(_.nonEmpty)
        .map(LocalDate.parse)

    statusOpt match {
      case Some(status) =>
        bookEntryRepository.updateStatusAndFinishedAt(entryId, status, finishedAtOpt)
          .map(_ => Redirect(routes.HomeController.editBookEntry(entryId)))

      case None =>
        Future.successful(BadRequest("Nieprawidłowy status"))
    }
  }
}
