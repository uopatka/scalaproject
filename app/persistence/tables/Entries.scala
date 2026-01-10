package persistence.tables

import models.Entry
import models.BookStatus
import models.EntryType
import java.time.LocalDateTime
import java.time.LocalDate

import slick.jdbc.PostgresProfile.api._

class Entries(tag: Tag) extends Table[Entry](tag, "entries") {
    import persistence.SlickColumnMappers._
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def entryType = column[EntryType]("entry_type")
    def refId = column[String]("ref_id")
    def createdAt = column[LocalDateTime]("created_at")
    def status = column[BookStatus]("status")
    def pagesRead = column[Int]("pages_read")
    def altCover = column[String]("alt_cover") // corresponding to altCover in the model
    def finishedAt = column[Option[LocalDate]]("finished_at")

  def pk = primaryKey("entries_pkey", id)
  def * = (id, userId, entryType, refId, createdAt, status, pagesRead, altCover, finishedAt) <> (Entry.tupled, Entry.unapply)
}

object Entries {
  val table = TableQuery[Entries]
}
