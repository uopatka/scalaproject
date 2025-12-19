package models

case class Book(isbn: String, title: String,
                author: String, publish_year: Int = 0,
                pages: Int = 0, cover: String = "")
