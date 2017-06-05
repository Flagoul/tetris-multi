package controllers

import javax.inject.Inject

import controllers.authentication.SecurityController
import forms.LoginForm._
import managers.{SessionManager, UserManager}
import models.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.{ExecutionContext, Future}


/**
  * Session controller
  *
  * @param users manager to access user's information
  * @param sessions manager to access session's information
  * @param messagesApi for localization
  * @param ec execution context in which to run
  */
class Session @Inject()(val users: UserManager, val sessions: SessionManager, val messagesApi: MessagesApi)
                       (implicit val ec: ExecutionContext)
  extends SecurityController with I18nSupport {

  /**
    * Get the login view.
    *
    * @return the login view
    */
  def login(): Action[AnyContent] = UnAuthenticatedAction { implicit request =>
    Ok(views.html.login(form))
  }

  /**
    * Handle the login form submission.
    *
    * @return the login form with errors or redirects to the main page.
    */
  def loginPost(): Action[AnyContent] = UnAuthenticatedAction.async { implicit request =>
    form.bindFromRequest.fold(
      errors => {
        Future.successful(BadRequest(views.html.login(errors)))
      },
      data => {
        users.authenticate(User(None, data.name, data.password))
          .flatMap {
            case Some(user) =>
              sessions.registerSession(user.id.get).map(session => Redirect("/").withSession(session))
            case None => Future.successful(
              BadRequest(views.html.login(form.withGlobalError("Bad password or user does not exist")))
            )
          }
      })
  }

  /**
    * Logout the user.
    *
    * @return a redirection to the login page.
    */
  def logout(): Action[AnyContent] = AuthenticatedAction.async { implicit request =>
    sessions.delete(request.userSession.get.id.get).map(
      _ => Redirect("/login").withSession(request.session - "uuid")
    )
  }
}
