package repositories

import models.{BookEntry, BookStatus}
import org.scalatestplus.play.PlaySpec

class BookEntryRepositorySpec extends PlaySpec {

  "BookEntryRepository" should {

    "start with default entries" in {
      val repo = new BookEntryRepository()
      repo.findAll().length must be > 0
    }

    "add a new entry correctly" in {
      val repo = new BookEntryRepository()
      val initialCount = repo.findAll().length
      val newId = repo.nextId()
      
      val newEntry = BookEntry(newId, 1, "1234567890")
      repo.add(newEntry)

      repo.findAll().length mustBe (initialCount + 1)
      repo.findById(newId) mustBe Some(newEntry)
    }
  }
}
