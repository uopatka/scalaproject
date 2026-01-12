package models

final case class Publication(doi: String, title: String,
                            authors: String, publishYear: Int = 0,
                            pages: String, cover: String = "") extends DisplayItem {
  def pageCount: Int = {
    if (pages == null || pages.isEmpty) return 0

    pages.split("-") match {
      case Array(startStr, endStr) =>
        (startStr.replaceAll("[^0-9]", ""), endStr.replaceAll("[^0-9]", "")) match {
          case (start, end) if start.nonEmpty && end.nonEmpty =>
            end.toInt - start.toInt + 1
          case _ => 0
        }
      case _ => 0
    }
  }
  def id: String = doi
  def year: Int = publishYear
}