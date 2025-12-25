package persistence

import models.User
import persistence.tables.Users
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import javax.inject._

@Singleton
class UserRepository @Inject()(db: Database)(implicit ec: ExecutionContext) {
  private val table = TableQuery[Users]

  def insert(user: User): Future[Long] =
    db.run((table returning table.map(_.id)) += user)

  def getById(id: Long): Future[Option[User]] =
    db.run(table.filter(_.id === id).result.headOption)

  def getByUsername(username: String): Future[Option[User]] =
    db.run(table.filter(_.username === username).result.headOption)

  def getAll(): Future[Seq[User]] =
    db.run(table.result)

  def update(user: User): Future[Int] =
    db.run(table.filter(_.id === user.id).update(user))

  def delete(id: Long): Future[Int] =
    db.run(table.filter(_.id === id).delete)
}