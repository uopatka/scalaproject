package services

import javax.inject._
import play.api.libs.ws.WSClient
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import models._

@Singleton
class OpenLibraryService @Inject() (
                                     ws: WSClient
                                   )(implicit ec: ExecutionContext) {

  private val baseUrl = "https://openlibrary.org/api/books"

  def fetchByIsbn(isbn: String): Future[Option[Book]] = {
    ws.url(baseUrl)
      .addQueryStringParameters(
        "bibkeys" -> s"ISBN:$isbn",
        "format"  -> "json",
        "jscmd"   -> "data"
      )
      .get()
      .map { response =>
        val key = s"ISBN:$isbn"

        (response.json \ key).validate[OpenLibraryBook].asOpt.map { olBook =>
          Book(
            isbn = isbn,
            title = olBook.title,
            author = olBook.authors.headOption.map(_.name).getOrElse(""),
            publishYear = olBook.publishDate.flatMap(_.take(4).toIntOption).getOrElse(0),
            pages = olBook.numberOfPages.getOrElse(0),
            cover = olBook.cover.flatMap(_.medium).getOrElse("")
          )
        }
      }
  }
}