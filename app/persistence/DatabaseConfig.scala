package persistence

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext

object DatabaseConfig {
  val db = Database.forConfig("db")
  implicit val ec: ExecutionContext = ExecutionContext.global
}