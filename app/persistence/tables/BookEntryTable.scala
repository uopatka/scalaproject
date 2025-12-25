package persistence.tables

import models.BookEntry
import models.BookStatus
import java.time.LocalDateTime

import slick.jdbc.PostgresProfile.api._

class BookEntries(tag: Tag) extends Table[BookEntry](tag, "book_entries") {
    import persistence.SlickColumnMappers._
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def isbn = column[String]("isbn")
    def createdAt = column[LocalDateTime]("created_at")
    def status = column[BookStatus]("status")
    def pagesRead = column[Int]("pages_read")

  def * = (id, userId, isbn, createdAt, status, pagesRead) <> (BookEntry.tupled, BookEntry.unapply)
}

object BookEntries {
  val table = TableQuery[BookEntries]
}
