package controllers

import javax.inject.Inject

import controllers.authentication.AuthenticatedController
import forms.LoginForm._
import managers.{SessionManager, UserManager}
import models.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.{ExecutionContext, Future}


class Login @Inject() (val users: UserManager, val sessions: SessionManager, val messagesApi: MessagesApi)
                      (implicit val ec: ExecutionContext)
  extends Controller with I18nSupport with AuthenticatedController {

  def index() = UnAuthenticatedAction { implicit request =>
    Ok(views.html.login(form))
  }

  def post(): Action[AnyContent] = UnAuthenticatedAction.async { implicit request =>
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
}
