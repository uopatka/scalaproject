package forms

import play.api.data._
import play.api.data.Forms._

case class RegisterData(
  username: String,
  password: String
)

object RegisterForm {
  val form: Form[RegisterData] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(RegisterData.apply)(RegisterData.unapply)
  )
}
