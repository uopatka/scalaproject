package repositories

import models.BookEntry

class BookEntryRepository {
  // Fake books for now
  private var bookEntries = List(
    BookEntry(1, 1, "9787201106380"),
    BookEntry(2, 1, "9788393535873"),
    BookEntry(3, 2, "9788393535873"),
    BookEntry(4, 3, "9788393535873"),
    BookEntry(5, 1, "9780008668808"),
    BookEntry(6, 1, "9780063277304")
  )

  def findAll(): List[BookEntry] =
    bookEntries

  def findById(id: Long): Option[BookEntry] =
    bookEntries.find(_.id == id)


  def add(bookEntry: BookEntry): Unit =
    bookEntries = bookEntries :+ bookEntry

  def update(entry: BookEntry): Unit =
    bookEntries = bookEntries.map {
      case e if e.id == entry.id => entry
      case e => e
    }

  def nextId(): Long = {
    if (bookEntries.isEmpty) 1
    else bookEntries.map(_.id).max + 1
  }
}
