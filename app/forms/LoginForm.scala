package forms

import play.api.data._
import play.api.data.Forms._

case class LoginData(username: String, password: String)

object LoginForm {
  val form: Form[LoginData] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )
}