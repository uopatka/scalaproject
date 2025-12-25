package persistence.tables

import models.Book
import slick.jdbc.PostgresProfile.api._

class Books(tag: Tag) extends Table[Book](tag, "books") {
  def isbn = column[String]("isbn")
  def title = column[String]("title")
  def author = column[String]("author")
  def publishYear = column[Int]("publish_year")
  def pages = column[Int]("pages")
  def cover = column[String]("cover")

  def * = (isbn, title, author, publishYear, pages, cover) <> (Book.tupled, Book.unapply)
}

object Books {
  val table = TableQuery[Books]
}
