package controllers

import javax.inject.Inject

import managers.UserManager
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import forms.LoginForm._
import models.User

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global



class Login @Inject()(users: UserManager, val messagesApi: MessagesApi) extends Controller with I18nSupport {
  def index() = Action { implicit request =>
    Ok(views.html.login(form))
  }

  def post() = Action.async { implicit request =>
    form.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(views.html.login(errors)))
      },
      data => {
        users.authenticate(User(None, data.name, data.password))
          .map {
            case Some(user) => Redirect("/")
            case None => BadRequest(views.html.login(form.withGlobalError("Bad password or user does not exist")))
          }
      })
  }
}
