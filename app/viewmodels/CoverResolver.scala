package viewmodels

import models.{Book, Entry}

object CoverResolver {
  private val uploadsBaseUrl = "/uploads"

  def resolve(entry: Entry, book: Book): String = {
    if (entry.altCover.nonEmpty) s"$uploadsBaseUrl/${entry.altCover}"
    else if (book.cover.startsWith("http://") || book.cover.startsWith("https://")) book.cover
    else if (book.cover.nonEmpty) s"$uploadsBaseUrl/${book.cover}"
    else "/assets/images/placeholder_cover.png"
  }
}
