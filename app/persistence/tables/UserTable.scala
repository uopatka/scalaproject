package persistence.tables

import models.User
import slick.jdbc.PostgresProfile.api._

class Users(tag: Tag) extends Table[User](tag, "users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username = column[String]("username")
  def password = column[String]("password")

  def * = (id, username, password) <> (User.tupled, User.unapply)
}

object Users {
  val table = TableQuery[Users]
}