package models

final case class Book(isbn: String, title: String,
                      author: String, publishYear: Int = 0,
                      pages: Int = 0, cover: String = "") extends DisplayItem {
  def authors: String = author
  def id: String = isbn
  def pageCount: Int = pages
  def year: Int = publishYear
}
