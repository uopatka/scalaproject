package utils

import models.{DisplayItem, Book, BookStatus, Entry}
import java.nio.file.Paths

object BookUtils {
  def ensureCover(book: Book): Book =
    if (book.cover.isEmpty) book.copy(cover = "/assets/images/placeholder_cover.png")
    else book

  def filterAndSortItems[T <: DisplayItem](
      entries: List[Entry],       // user entries
      items: Seq[T],              // books and publications
      filters: Map[String, String]
  ): (List[Entry], List[T]) = {

    val statusFilter: Option[BookStatus] =
      filters.get("status").flatMap(s => BookStatus.fromString(s.toLowerCase))

    val searchQuery: Option[String] =
      filters.get("search").map(_.trim).filter(_.nonEmpty)

    val sortParam: String = filters.getOrElse("sort", "title_asc")
    val (sortField, sortOrder) = sortParam.split("_") match {
      case Array(f, o) => (f.toLowerCase, o.toLowerCase)
      case Array(f)    => (f.toLowerCase, "asc")
      case _           => ("title", "asc")
    }

    val filteredEntries = statusFilter match {
      case Some(status) => entries.filter(_.status == status)
      case None         => entries
    }

    val itemIds = filteredEntries.map(_.refId).toSet
    val filteredItems = items.filter(i => itemIds.contains(i.id)).toList

    val searchFilteredEntries = searchQuery match {
      case Some(query) =>
        val q = query.toLowerCase
        filteredEntries.filter { entry =>
          filteredItems.find(_.id == entry.refId).exists { item =>
            item.title.toLowerCase.contains(q) || item.authors.toLowerCase.contains(q)
          }
        }
      case None => filteredEntries
    }

    val finalFilteredItems = filteredItems.filter { item =>
      searchFilteredEntries.exists(_.refId == item.id)
    }

    val sortedItems = (sortField, sortOrder) match {
      case ("title", "asc")   => finalFilteredItems.sortBy(_.title)
      case ("title", "desc")  => finalFilteredItems.sortBy(_.title)(Ordering[String].reverse)
      case ("author", "asc")  => finalFilteredItems.sortBy(_.authors)
      case ("author", "desc") => finalFilteredItems.sortBy(_.authors)(Ordering[String].reverse)
      case ("year", "asc")    => finalFilteredItems.sortBy(_.year)
      case ("year", "desc")   => finalFilteredItems.sortBy(_.year)(Ordering[Int].reverse)
      case _                  => finalFilteredItems
    }

    val sortedEntries = searchFilteredEntries.sortBy(e => sortedItems.indexWhere(_.id == e.refId))

    (sortedEntries, sortedItems)
  }
}
