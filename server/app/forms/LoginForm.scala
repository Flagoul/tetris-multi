package forms

import play.api.data._
import play.api.data.Forms._


/**
  * Contains data useful for login a user in
  */
object LoginForm {
  /**
    * Class to encapsulate login data
    *
    * @param name of the user
    * @param password of the user
    */
  case class LoginData(name: String, password: String)

  /**
    * Form for login
    */
  val form = Form(
    mapping(
      "username" -> nonEmptyText(maxLength = 255),
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )
}
