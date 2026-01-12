package persistence.tables

import models.Publication
import slick.jdbc.PostgresProfile.api._

class Publications(tag: Tag) extends Table[Publication](tag, "publications") {
  
  def doi = column[String]("doi")
  def title = column[String]("title")
  def authors = column[String]("authors")
  def publishYear = column[Int]("publish_year")
  def pages = column[String]("pages")
  def cover = column[String]("cover")

  def pk = primaryKey("publications_pkey", doi)

  def * = (doi, title, authors, publishYear, pages, cover) <> (Publication.tupled, Publication.unapply)

}

object Publications {
  val table = TableQuery[Publications]
}