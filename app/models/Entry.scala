package models

import java.time.{LocalDate, LocalDateTime}

final case class Entry(
  id: Long,
  userId: Long,
  entryType: EntryType,
  refId: String,        // ISBN or DOI
  createdAt: LocalDateTime = LocalDateTime.now(),
  status: BookStatus = BookStatus.ToRead,
  pagesRead: Int = 0,
  altCover: String = "",
  finishedAt: Option[LocalDate] = None
)
