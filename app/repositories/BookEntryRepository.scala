package repositories

import models.BookEntry

import javax.inject._
import java.time.LocalDateTime

class BookEntryRepository {
  // Fake books for now
  private var bookEntries = List(
    BookEntry(1, 1, "9787201106380", LocalDateTime.now(), "to read", 0),
    BookEntry(2, 1, "9780451524935", LocalDateTime.now(), "to read", 0)
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
