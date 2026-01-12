package services

import javax.inject._
import play.api.libs.ws.WSClient
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import models._

@Singleton
class CrossrefService @Inject() (
                                  ws: WSClient
                                )(implicit ec: ExecutionContext) {

  private val baseUrl = "https://api.crossref.org/works"


  def fetchByDoi(doi: String): Future[Option[Publication]] = {
    ws.url(s"$baseUrl/$doi")
      .get()
      .map { response =>
        val item = response.json \ "message"
        
        val title = (item \ "title").asOpt[JsArray]
          .flatMap(_.value.headOption.flatMap(_.asOpt[String]))
          .getOrElse("")
        
        val authors = (item \ "author").asOpt[JsArray]
          .map { authorsArray =>
            authorsArray.value.map { author =>
              val `given` = (author \ "given").asOpt[String].getOrElse("")
              val family = (author \ "family").asOpt[String].getOrElse("")
              s"$given $family".trim
            }.mkString(", ")
          }
          .getOrElse("Unknown")
        
        val year = (item \ "published-print" \ "date-parts").asOpt[JsArray]
          .orElse((item \ "published-online" \ "date-parts").asOpt[JsArray])
          .flatMap(_.value.headOption)
          .flatMap(_.asOpt[JsArray])
          .flatMap(_.value.headOption)
          .flatMap(_.asOpt[Int])
          .getOrElse(0)
        
        val pages = (item \ "page").asOpt[String].getOrElse("")
        
        if (title.nonEmpty) {
          Some(Publication(
            doi = doi,
            title = title,
            authors = authors,
            publishYear = year,
            pages = pages,
            cover = ""
          ))
        } else {
          None
        }
      }
      .recover {
        case e: Exception =>
          println(s"Failed to fetch from Crossref: ${e.getMessage}")
          None
      }
  }

}