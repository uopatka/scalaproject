package persistence

import slick.jdbc.PostgresProfile.api._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import models.{BookStatus, EntryType}
import java.time.LocalDateTime

object SlickColumnMappers {

  // LocalDateTime <-> java.sql.Timestamp
  implicit val localDateTimeColumnType: JdbcType[LocalDateTime] with BaseTypedType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, java.sql.Timestamp](
      ldt => java.sql.Timestamp.valueOf(ldt),
      ts => ts.toLocalDateTime
    )

  // BookStatus <-> String
  implicit val bookStatusColumnType: JdbcType[BookStatus] with BaseTypedType[BookStatus] =
    MappedColumnType.base[BookStatus, String](
      bs => bs.value,
      str => BookStatus.fromString{str}.getOrElse(throw new IllegalArgumentException(s"Invalid BookStatus: $str"))
    )

  // EntryType <-> String
  implicit val entryTypeColumnType: JdbcType[models.EntryType] with BaseTypedType[EntryType] =
    MappedColumnType.base[models.EntryType, String](
      et => et.value,
      str => models.EntryType.fromString(str)
    )
}