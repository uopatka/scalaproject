package services

import javax.inject._
import play.api.libs.ws.WSClient
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import models._
import java.nio.file.Paths
import java.nio.file.Files
import play.api.Configuration

@Singleton
class OpenLibraryService @Inject() (
                                     ws: WSClient,
                                     config: Configuration
                                   )(implicit ec: ExecutionContext) {

  private val baseUrl = "https://openlibrary.org/api/books"

  private def downloadCover(url: String, isbn: String): Future[Option[String]] = {
    val filename = s"${isbn}.jpg"
    val directory = Paths.get(config.get[String]("app.uploads.dir"))
    
    try {
      Files.createDirectories(directory)
      val path = directory.resolve(filename)
      
      val connection = new java.net.URL(url).openConnection()
      connection.setRequestProperty("User-Agent", "Bugshelv/1.0")
      
      val inputStream = connection.getInputStream
      try {
        Files.copy(inputStream, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        Future.successful(Some(filename))
      } finally {
        inputStream.close()
      }
    } catch {
      case e: Exception =>
        println(s"Failed to download cover: ${e.getMessage}")
        Future.successful(None)
    }
  }

  def fetchByIsbn(isbn: String): Future[Option[Book]] = {
    ws.url(baseUrl)
      .addQueryStringParameters(
        "bibkeys" -> s"ISBN:$isbn",
        "format"  -> "json",
        "jscmd"   -> "data"
      )
      .get()
      .flatMap { response =>
        val key = s"ISBN:$isbn"

        (response.json \ key).validate[OpenLibraryBook].asOpt match {
          case Some(olBook) =>
            val coverUrlOpt = olBook.cover.flatMap(_.medium)

            val coverFuture: Future[String] = coverUrlOpt match {
              case Some(coverUrl) =>
                downloadCover(coverUrl, isbn).map {
                  case Some(filename) => filename
                  case None           => ""
                }
              case None =>
                Future.successful("")
            }
            
            coverFuture.map { coverFilename =>
              Some(Book(
                isbn = isbn,
                title = olBook.title,
                author = olBook.authors.headOption.map(_.name).getOrElse(""),
                publishYear = olBook.publishDate.flatMap(_.take(4).toIntOption).getOrElse(0),
                pages = olBook.numberOfPages.getOrElse(0),
                cover = coverFilename
              ))
            }
          case None =>  
            Future.successful(None)
        }
      }
  }
}