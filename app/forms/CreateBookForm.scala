package forms

import play.api.data._
import play.api.data.Forms._

case class BookData(isbn: String, title: String,
                author: String, publishYear: Int = 0,
                pages: Int = 0, cover: String = "")

object CreateBookForm {
  val form: Form[BookData] = Form(
    mapping(
      "isbn" -> nonEmptyText.verifying(
        "Nieprawidłowy ISBN",
        isbn => isbn.replaceAll("-", "").matches("""\d{10}|\d{13}""")),
      "title" -> nonEmptyText.verifying("Pole tytuł nie może być puste", _.nonEmpty),
      "author" -> nonEmptyText.verifying("Pole autor nie może być pusty", _.nonEmpty),
      "publishYear" -> default(number, 0),
      "pages" -> default(number, 0),
      "cover" -> default(text, "")
    )(BookData.apply)(BookData.unapply)
  )
}