package models

import play.api.libs.json._
import play.api.libs.functional.syntax._


final case class OpenLibraryAuthor(name: String)
final case class OpenLibraryCover(medium: Option[String])

final case class OpenLibraryBook(
                            title: String,
                            authors: Seq[OpenLibraryAuthor],
                            publishDate: Option[String],
                            numberOfPages: Option[Int],
                            cover: Option[OpenLibraryCover]
                          )

object OpenLibraryBook {
  implicit val authorReads: Reads[OpenLibraryAuthor] = Json.reads
  implicit val coverReads: Reads[OpenLibraryCover] = Json.reads

  // custom Reads, so we can keep camel case notation
  implicit val bookReads: Reads[OpenLibraryBook] = (
    (JsPath \ "title").read[String] and
      (JsPath \ "authors").read[Seq[OpenLibraryAuthor]] and
      (JsPath \ "publish_date").readNullable[String] and    // "publish_date" → publishDate
      (JsPath \ "number_of_pages").readNullable[Int] and   //  "number_of_pages" → numberOfPages
      (JsPath \ "cover").readNullable[OpenLibraryCover]
    )(OpenLibraryBook.apply _)
}
