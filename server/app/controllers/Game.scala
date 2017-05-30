package controllers

import play.api.mvc.{Action, Controller}

/**
  * Temporary controller rendering game, for development purpose.
  */
class Game extends Controller {
  def index() = Action { implicit request =>
    Ok(views.html.game())
  }
}
