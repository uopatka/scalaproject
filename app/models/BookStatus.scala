package models

sealed trait BookStatus {
  def value: String
}

object BookStatus {
  case object ToRead   extends BookStatus { val value = "to_read" }
  case object Reading  extends BookStatus { val value = "reading" }
  case object Dropped  extends BookStatus { val value = "dropped" }
  case object Finished extends BookStatus { val value = "finished" }

  val all: List[BookStatus] = List(ToRead, Reading, Dropped, Finished)

  def fromString(s: String): Option[BookStatus] =
    all.find(_.value == s)

  def label(status: BookStatus): String = status match {
    case ToRead   => "Do przeczytania"
    case Reading  => "W trakcie"
    case Finished => "Przeczytane"
    case Dropped  => "Porzucone"
  }
}
