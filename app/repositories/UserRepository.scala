package repositories

import javax.inject._
import models.User
import scala.concurrent.{Future, ExecutionContext}

@Singleton
class UserRepository @Inject()()(implicit ec: ExecutionContext) {

  private val users = Seq(
    User(1, "ola", "admin1"), // password: secret
  )

  def findByUsername(username: String): Future[Option[User]] =
    Future.successful(users.find(_.username == username))
}