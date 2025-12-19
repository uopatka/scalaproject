package services

import javax.inject._
import models.{BookEntry, BookStatus}
import repositories.BookEntryRepository

@Singleton
class BookEntryService @Inject()(repo: BookEntryRepository) {

  def changeStatus(entry: BookEntry, newStatus: BookStatus): BookEntry = {
    val updated = entry.copy(status = newStatus)
    repo.update(updated)
    updated
  }

  def updatePagesRead(entry: BookEntry, pages: Int): BookEntry = {
    val updated = entry.copy(pagesRead = pages)
    repo.update(updated)
    updated
  }

  def markAsFinished(entry: BookEntry): BookEntry = {
    val updated = entry.copy(status = BookStatus.Finished)
    repo.update(updated)
    updated
  }

  def findById(id: Long): Option[BookEntry] = repo.findById(id)

  def addEntry(entry: BookEntry): BookEntry = {
    repo.add(entry)
    entry
  }

}