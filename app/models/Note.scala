package models

import java.time.LocalDateTime

case class Note(
                 id: Long,
                 bookEntryId: Long,
                 userId: Long,
                 title: String,
                 content: String,
                 createdAt: LocalDateTime = LocalDateTime.now(),
                 updatedAt: LocalDateTime = LocalDateTime.now()
               )
