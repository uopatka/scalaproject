package persistence

import models.BookEntry
import persistence.tables.BookEntries
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Future

class BookEntryRepository(db: Database) {
  private val table = TableQuery[BookEntries]

  def insert(entry: BookEntry): Future[Long] =
    db.run((table returning table.map(_.id)) += entry)

  def getById(id: Long): Future[Option[BookEntry]] =
    db.run(table.filter(_.id === id).result.headOption)

  def getAll(): Future[Seq[BookEntry]] =
    db.run(table.result)

  def update(entry: BookEntry): Future[Int] =
    db.run(table.filter(_.id === entry.id).update(entry))

  def delete(id: Long): Future[Int] =
    db.run(table.filter(_.id === id).delete)
}

