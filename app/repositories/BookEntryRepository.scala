package repositories

import models.{Entry, BookStatus, EntryType}

import java.time.LocalDateTime

class EntryRepository {
  // Fake books for now
  private var entries = List(
    Entry(1, 1, EntryType.Book, "9787201106380"),
    Entry(2, 1, EntryType.Book, "9788393535873"),
    Entry(3, 2, EntryType.Book, "9788393535873", LocalDateTime.now(), BookStatus.Finished, 2),
    Entry(4, 3, EntryType.Book, "9788393535873"),
    Entry(5, 1, EntryType.Book, "9780008668808"),
    Entry(6, 1, EntryType.Book, "9780063277304")
  )

  def findAll(): List[Entry] =
    entries

  def findById(id: Long): Option[Entry] =
    entries.find(_.id == id)

  def add(entry: Entry): Unit =
    entries = entries :+ entry

  def update(entry: Entry): Unit =
    entries = entries.map {
      case e if e.id == entry.id => entry
      case e => e
    }

  def nextId(): Long = {
    if (entries.isEmpty) 1
    else entries.map(_.id).max + 1
  }
}
