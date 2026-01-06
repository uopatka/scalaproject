package persistence.tables

import models.Note
import java.time.LocalDateTime
import persistence.tables.Entries
import slick.jdbc.PostgresProfile.api._




class Notes(tag: Tag) extends Table[Note](tag, "notes") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def entryId = column[Long]("entry_id")
  def userId = column[Long]("user_id")
  def title = column[String]("title")
  def content = column[String]("content")
  def createdAt = column[LocalDateTime]("created_at")
  def updatedAt = column[LocalDateTime]("updated_at")

  def * = (id, entryId, userId, title, content, createdAt, updatedAt) <> (Note.tupled, Note.unapply)

  def entry =
    foreignKey("fk_note_entry", entryId, Entries.table)(_.id, onDelete = ForeignKeyAction.Cascade)
}

object Notes {
  val table = TableQuery[Notes]
}
