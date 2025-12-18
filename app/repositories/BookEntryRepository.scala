package repositories

import models.BookEntry

import javax.inject._
import java.time.LocalDateTime

class BookEntryRepository {
  // Fake books for now
  private var bookEntries = List(
    BookEntry(1, 1, "9787201106380", LocalDateTime.now()),
    BookEntry(2, 1, "9780451524935", LocalDateTime.now())
  )

  def findAll(): List[BookEntry] =
    bookEntries

  def add(bookEntry: BookEntry): Unit =
    bookEntries = bookEntries :+ bookEntry
}
