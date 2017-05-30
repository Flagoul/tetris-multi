package controllers

import java.sql.SQLIntegrityConstraintViolationException
import javax.inject.Inject

import forms.RegistrationForm._
import managers.UserManager
import models.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}


class Registration @Inject()(users: UserManager, val messagesApi: MessagesApi)
                            (implicit ec: ExecutionContext)extends Controller with I18nSupport {
  def index() = Action { implicit request =>
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
          .recover({
            case exception: SQLIntegrityConstraintViolationException =>
              if (exception.getMessage.contains("Duplicate") && exception.getMessage.contains("username")) {
                BadRequest(views.html.registration(form.withError("username", "This username is already taken")))
              } else {
                // FIXME: we should log this
                InternalServerError("Oops ! Something bad happened while saving you to the database. Please retry.")
              }
          })
      }
    )
  }
}
