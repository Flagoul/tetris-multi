package controllers

import java.sql.SQLIntegrityConstraintViolationException
import javax.inject.Inject

import controllers.authentication.SecurityController
import forms.RegistrationForm._
import managers.{SessionManager, UserManager}
import models.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.{ExecutionContext, Future}


class Registration @Inject()(users: UserManager, val sessions: SessionManager, val messagesApi: MessagesApi)
                            (implicit val ec: ExecutionContext)
  extends SecurityController with I18nSupport {

  def index() = UnAuthenticatedAction { implicit request =>
    Ok(views.html.registration(form))
  }

  def post(): Action[AnyContent] = UnAuthenticatedAction.async { implicit request =>
    form.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(views.html.registration(errors)))
      },
      data => {
        users.create(User(None, data.name, data.password))
          .flatMap((user: User) =>
            sessions.registerSession(user.id.get).map(session => Redirect("/").withSession(session))
          )
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
