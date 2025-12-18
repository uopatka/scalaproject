package repositories

import models.Book

import javax.inject._

@Singleton
class BookRepository {

  // Fake books for now
  private var books = List(
    Book("9787201106380", "1984", "George Orwell", 222, 2233, "2232"),
    Book("9780451524935", "Pożegnanie Słońca", "Maciej Gorywoda", 222, 2233, "2232")
  )

  def findAll(): List[Book] =
    books

  def add(book: Book): Unit =
    books = books :+ book
}