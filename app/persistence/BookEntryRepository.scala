package persistence

import models.Entry
import models.BookStatus
import persistence.SlickColumnMappers._
import persistence.tables.Entries
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import javax.inject._
import java.time.LocalDate


@Singleton
class BookEntryRepository @Inject()(db: Database)(implicit ec: ExecutionContext) {
  private val table = TableQuery[Entries]

  def insert(entry: Entry): Future[Long] =
    db.run((table returning table.map(_.id)) += entry)

  def getById(id: Long): Future[Option[Entry]] =
    db.run(table.filter(_.id === id).result.headOption)

  def getAll(): Future[Seq[Entry]] =
    db.run(table.result)

  def update(entry: Entry): Future[Int] =
    db.run(table.filter(_.id === entry.id).update(entry))

  def delete(id: Long): Future[Int] =
    db.run(table.filter(_.id === id).delete)

  def updatePagesRead(id: Long, pagesRead: Int): Future[Int] =
    db.run(table.filter(_.id === id).map(_.pagesRead).update(pagesRead))

  def updateStatus(id: Long, status: BookStatus): Future[Int] =
    db.run(table.filter(_.id === id).map(_.status).update(status))

  def updateStatusAndFinishedAt(id: Long, status: BookStatus, finishedAt: Option[LocalDate]): Future[Int] = {
    val finishedValueSql = finishedAt match {
      case Some(date) if status == BookStatus.Finished => s"'${date.toString}'"
      case _ => "NULL"
    }

    val sql =
      sqlu"""
      UPDATE entries
      SET status = ${status.value},
          finished_at = #$finishedValueSql
      WHERE id = $id
    """

    db.run(sql)
  }

}

