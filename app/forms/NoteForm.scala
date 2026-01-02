package forms

import play.api.data._
import play.api.data.Forms._

case class NoteData(title: String, content: String)

object NoteForm {
  val form: Form[NoteData] = Form(
    mapping(
      "title" -> nonEmptyText,
      "content" -> nonEmptyText
    )(NoteData.apply)(NoteData.unapply)
  )
}
