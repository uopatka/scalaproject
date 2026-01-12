package viewmodels

import models.{Book, Entry}
import models.DisplayItem

object CoverResolver {
  private val uploadsBaseUrl = "/uploads"

  def resolve(entry: Entry, item: DisplayItem): String = {
    if (entry.altCover.nonEmpty) s"$uploadsBaseUrl/${entry.altCover}"
    else if (item.cover.startsWith("http://") || item.cover.startsWith("https://")) item.cover
    else if (item.cover.nonEmpty) s"$uploadsBaseUrl/${item.cover}"
    else "/assets/images/placeholder_cover.png"
  }
}
