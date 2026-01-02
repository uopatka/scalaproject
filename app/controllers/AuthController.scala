package controllers

import play.api.mvc._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

import forms.{LoginForm, RegisterForm}
import models.User
import persistence.UserRepository

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

  def registerPage = Action { implicit request =>
    Ok(views.html.register(RegisterForm.form))
  }

  def registerSubmit = Action.async { implicit request =>
    RegisterForm.form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(
          BadRequest(views.html.register(formWithErrors))
        ),

      registerData => {
        userRepo.getByUsername(registerData.username).flatMap {
          case Some(_) =>
            val formWithError =
              RegisterForm.form.withGlobalError("Username already exists")
            Future.successful(
              BadRequest(views.html.register(formWithError))
            )

          case None =>
            userRepo.insert(
              User(
                id = 0L,
                username = registerData.username,
                password = registerData.password // ❗ plain text (temporary)
              )
            ).map { _ =>
              Redirect("/login")
            }
        }
      }
    )
  }
}