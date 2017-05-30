package forms

import play.api.data._
import play.api.data.Forms._


case class LoginData(name: String, password: String)


object LoginForm {
  def validate(data: UserRegistrationData) = {
    data.password match {
      case data.password_confirmation => Some(data)
      case _ => None
    }
  }

  val form = Form(
    mapping(
      "username" -> nonEmptyText(maxLength = 255),
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )
}
