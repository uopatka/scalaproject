package repositories

import org.scalatestplus.play.PlaySpec
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.ExecutionContext.Implicits.global

class UserRepositorySpec extends PlaySpec with ScalaFutures {

  "UserRepository" should {

    "find a user by username asynchronously" in {
      val repo = new UserRepository()
      
      val resultFuture = repo.findByUsername("ola")

      whenReady(resultFuture) { userOption =>
        userOption must be(defined)
        userOption.get.username mustBe "ola"
      }
    }

    "return None for unknown users" in {
      val repo = new UserRepository()
      val resultFuture = repo.findByUsername("unknown_ghost")

      whenReady(resultFuture) { userOption =>
        userOption mustBe empty
      }
    }
  }
}
