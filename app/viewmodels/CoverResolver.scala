package viewmodels

import models.{Book, BookEntry}

object CoverResolver {
  private val uploadsBaseUrl = "/uploads"

  def resolve(entry: BookEntry, book: Book): String = {
    if (entry.altCover.nonEmpty) s"$uploadsBaseUrl/${entry.altCover}"
    else if (book.cover.nonEmpty) book.cover
    else "/assets/images/placeholder_cover.png"
  }
}
