package models

final case class Publication(doi: String, title: String,
                            authors: String, publishYear: Int = 0,
                            pages: String, cover: String = "")
