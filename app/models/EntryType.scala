package models

sealed trait EntryType {
  def value: String
}

object EntryType {
  case object Book extends EntryType {
    val value: String = "book"
  }

  case object Publication extends EntryType {
    override val value: String = "publication"
  }

  def fromString(s: String): EntryType = s.toLowerCase match {
    case "book"        => Book
    case "publication" => Publication
    case other         => throw new IllegalArgumentException(s"Unknown entry type: $other")
  }

  val all: List[EntryType] = List(Book, Publication)
}
