package controllers

import java.util.UUID
import javax.inject.Inject

import managers.{SessionManager, UserManager}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import forms.LoginForm._
import models.{Session, User}

import scala.concurrent.{ExecutionContext, Future}


class Login @Inject()(users: UserManager, sessions: SessionManager, val messagesApi: MessagesApi)
                     (implicit ec: ExecutionContext) extends Controller with I18nSupport {

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
          .flatMap {
            case Some(user) =>
              sessions.create(Session(None, UUID.randomUUID.toString, None, user.id.get))
                .map(session => Redirect("/").withSession(request.session + ("uuid" -> session.uuid.toString)))
            case None => Future.successful(
              BadRequest(views.html.login(form.withGlobalError("Bad password or user does not exist")))
            )
          }
      })
  }
}
