package controllers

import java.sql.SQLIntegrityConstraintViolationException
import javax.inject.Inject

import controllers.authentication.SecurityController
import forms.RegistrationForm._
import managers.{SessionManager, UserManager}
import models.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.Logger.logger


import scala.concurrent.{ExecutionContext, Future}


/**
  * Registration controller
  *
  * @param users manager to handle users
  * @param sessions manager to handle sessions
  * @param messagesApi to get internationalization
  * @param ec execution context in which to run
  */
class Registration @Inject()(users: UserManager, val sessions: SessionManager, val messagesApi: MessagesApi)
                            (implicit val ec: ExecutionContext)
  extends SecurityController with I18nSupport {

  /**
    * Get the main page for registration.
    *
    * @return the register page
    */
  def index() = UnAuthenticatedAction { implicit request =>
    Ok(views.html.registration(form))
  }

  /**
    * Handle post requests for registration.
    *
    * @return an error with a new form or redirects to the index page
    */
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
            // FIXME : wrap this at the Manager level
            case exception: SQLIntegrityConstraintViolationException =>
              if (exception.getMessage.contains("Duplicate") && exception.getMessage.contains("username")) {
                BadRequest(views.html.registration(form.withError("username", "This username is already taken")))
              } else {
                logger.error(exception.toString)
                InternalServerError("Oops ! Something bad happened while saving you to the database. Please retry.")
              }
          })
      }
    )
  }
}
