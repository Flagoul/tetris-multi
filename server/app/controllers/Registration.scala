package controllers

import javax.inject.Inject

import play.api.mvc.{Action, Controller}
import forms.RegistrationForm._
import managers.UserManager
import models.User
import play.api.i18n.{I18nSupport, MessagesApi}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class Registration @Inject()(users: UserManager, val messagesApi: MessagesApi) extends Controller with I18nSupport {
  def index() = Action {
    Ok(views.html.registration(form))
  }

  def post() = Action.async { implicit request =>
    form.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(views.html.registration(errors)))
      },
      data => {
        users.create(User(None, data.name, data.password))
          .map(_ => Redirect("/"))
      }
    )
  }
}
