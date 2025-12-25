package controllers

import play.api.mvc._
import javax.inject._
import forms.LoginForm
import repositories.{UserRepository => TestUserRepository}
import persistence.UserRepository
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthController @Inject()(
                                cc: MessagesControllerComponents,
                                userRepo: UserRepository
                              )(implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

  def loginPage = Action { implicit request =>
    Ok(views.html.login(LoginForm.form))
  }

  def loginSubmit = Action.async { implicit request =>
    LoginForm.form.bindFromRequest().fold(
      formWithErrors => {

        Future.successful(BadRequest(views.html.login(formWithErrors)))
      },
      loginData => {
        userRepo.getByUsername(loginData.username).map {
          case Some(user) if user.password == loginData.password =>

            Redirect("/").withSession(
              "username" -> user.username,
              "userId" -> user.id.toString
            )
          case _ =>

            val formWithError = LoginForm.form.withGlobalError("Invalid username or password")
            BadRequest(views.html.login(formWithError))
        }
      }
    )
  }

  def logout = Action {
    Redirect("/").withNewSession
  }
}