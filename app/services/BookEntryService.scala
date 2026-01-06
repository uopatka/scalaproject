package services

import javax.inject._
import models.{Entry, BookStatus}
import repositories.EntryRepository

@Singleton
class BookEntryService @Inject()(repo: EntryRepository) {

  def changeStatus(entry: Entry, newStatus: BookStatus): Entry = {
    val updated = entry.copy(status = newStatus)
    repo.update(updated)
    updated
  }

  def updatePagesRead(entry: Entry, pages: Int): Entry = {
    val updated = entry.copy(pagesRead = pages)
    repo.update(updated)
    updated
  }

  def markAsFinished(entry: Entry): Entry = {
    val updated = entry.copy(status = BookStatus.Finished)
    repo.update(updated)
    updated
  }

  def findById(id: Long): Option[Entry] = repo.findById(id)

  def addEntry(entry: Entry): Entry = {
    repo.add(entry)
    entry
  }

}