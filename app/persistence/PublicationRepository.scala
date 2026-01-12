package persistence

import models.Publication
import persistence.tables.Publications
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import javax.inject._

@Singleton
class PublicationRepository @Inject()(db: Database)(implicit ec: ExecutionContext) {
  private val table = TableQuery[Publications]

  def insert(publication: Publication): Future[String] =
    db.run((table returning table.map(_.doi)) += publication)

  def getByDoi(doi: String): Future[Option[Publication]] =
    db.run(table.filter(_.doi === doi).result.headOption)

  def getAll(): Future[Seq[Publication]] =
    db.run(table.result)

  def update(publication: Publication): Future[Int] =
    db.run(table.filter(_.doi === publication.doi).update(publication))

  def delete(doi: String): Future[Int] =
    db.run(table.filter(_.doi === doi).delete)

  def upsert(publication: Publication): Future[Int] =
    db.run(table.insertOrUpdate(publication))
}
