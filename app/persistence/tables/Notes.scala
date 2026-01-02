package persistence.tables

import models.Note
import java.time.LocalDateTime
import persistence.tables.BookEntries
import slick.jdbc.PostgresProfile.api._




class Notes(tag: Tag) extends Table[Note](tag, "notes") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def bookEntryId = column[Long]("book_entry_id")
  def userId = column[Long]("user_id")
  def title = column[String]("title")
  def content = column[String]("content")
  def createdAt = column[LocalDateTime]("created_at")
  def updatedAt = column[LocalDateTime]("updated_at")

  def * = (id, bookEntryId, userId, title, content, createdAt, updatedAt) <> (Note.tupled, Note.unapply)

  def bookEntry =
    foreignKey("fk_note_entry", bookEntryId, BookEntries.table)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Notes {
  val table = TableQuery[Notes]
}
