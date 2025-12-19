package models

import java.time.LocalDateTime

case class BookEntry (id: Long, userId: Long, isbn: String,
                      createdAt: LocalDateTime = LocalDateTime.now(), status: BookStatus = BookStatus.ToRead,
                      pagesRead: Int = 0)