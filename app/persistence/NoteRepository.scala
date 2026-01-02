package persistence

import models.Note
import persistence.tables.Notes
import slick.jdbc.PostgresProfile.api._


import scala.concurrent.{Future, ExecutionContext}
import javax.inject._

@Singleton
class NoteRepository @Inject()(db: Database)(implicit ec: ExecutionContext) {

  private val table = Notes.table

  def create(note: Note): Future[Long] =
    db.run((table returning table.map(_.id)) += note)

  def findByBookEntry(bookEntryId: Long): Future[Seq[Note]] =
    db.run(table.filter(_.bookEntryId === bookEntryId).sortBy(_.createdAt.desc).result)

  def findById(id: Long): Future[Option[Note]] =
    db.run(table.filter(_.id === id).result.headOption)

  def update(note: Note): Future[Int] =
    db.run(table.filter(_.id === note.id).update(note.copy(updatedAt = java.time.LocalDateTime.now())))

  def delete(id: Long): Future[Int] =
    db.run(table.filter(_.id === id).delete)

}
