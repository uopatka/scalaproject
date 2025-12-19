package models

import java.time.LocalDateTime

case class BookEntry (id: Long, user_id: Long, isbn: String,
                      created_at: LocalDateTime, status: String,
                      pages_read: Integer)