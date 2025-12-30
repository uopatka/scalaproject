package viewmodels

import models.{Book, BookEntry}

object CoverResolver {

  def resolve(entry: BookEntry, book: Book): String = {
    if (entry.altCover.nonEmpty) entry.altCover
    else if (book.cover.nonEmpty) book.cover
    else "/assets/images/placeholder_cover.png"
  }
}
