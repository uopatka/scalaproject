package repositories

import models.{Entry, BookStatus, EntryType}
import org.scalatestplus.play.PlaySpec

class EntryRepositorySpec extends PlaySpec {

  "EntryRepository" should {

    "start with default entries" in {
      val repo = new EntryRepository()
      repo.findAll().length must be > 0
    }

    "add a new entry correctly" in {
      val repo = new EntryRepository()
      val initialCount = repo.findAll().length
      val newId = repo.nextId()
      
      val newEntry = Entry(newId, 1, EntryType.Book, "1234567890")
      repo.add(newEntry)

      repo.findAll().length mustBe (initialCount + 1)
      repo.findById(newId) mustBe Some(newEntry)
    }
  }
}
