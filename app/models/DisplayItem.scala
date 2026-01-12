package models

trait DisplayItem {
  def title: String
  def authors: String
  def pageCount: Int
  def year: Int
  def id: String  // isbn or doi for now
  def cover: String
}
