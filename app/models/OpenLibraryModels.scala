package models

import play.api.libs.json._

case class OpenLibraryAuthor(name: String)
case class OpenLibraryCover(medium: Option[String])

case class OpenLibraryBook(
                            title: String,
                            authors: Seq[OpenLibraryAuthor],
                            publishDate: Option[String],
                            numberOfPages: Option[Int],
                            cover: Option[OpenLibraryCover]
                          )

object OpenLibraryBook {
  implicit val authorReads: Reads[OpenLibraryAuthor] = Json.reads
  implicit val coverReads: Reads[OpenLibraryCover] = Json.reads
  implicit val bookReads: Reads[OpenLibraryBook] = Json.reads
}
