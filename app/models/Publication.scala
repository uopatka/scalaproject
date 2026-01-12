package models

final case class Publication(doi: String, title: String,
                            authors: String, publishYear: Int = 0,
                            pages: String, cover: String = "") extends DisplayItem {
  def pageCount: Int = {
    val pattern = """.*\((\d+) pages\).*""".r
    pages match {
      case pattern(count) => count.toInt
      case _ => 0
    }
  }
  def id: String = doi
  def year: Int = publishYear
}