package models

import java.time.LocalDateTime

case class BookEntry (val id: Int, val user_id: Int, val isbn: String, val created_at: LocalDateTime) {
  var status: String = "to read";
  var pages_read: Integer = 0;

  def changeStatus(newStatus: String): Unit = {
    status = newStatus;
  }

  def changePagesRead(currentPagesRead: Integer): Unit = {
    pages_read = currentPagesRead;
  }

}
