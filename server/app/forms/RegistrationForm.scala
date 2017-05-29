package forms

import play.api.data._
import play.api.data.Forms._


case class UserRegistrationData(name: String, password: String, password_confirmation: String)


object RegistrationForm {
  def validate(data: UserRegistrationData) = {
    data.password match {
      case data.password_confirmation => Some(data)
      case _ => None
    }
  }

  val form = Form(
    mapping(
      "username" -> nonEmptyText(maxLength = 255),
      "password" -> nonEmptyText,
      "password_confirmation" -> nonEmptyText
    )(UserRegistrationData.apply)(UserRegistrationData.unapply)
      .verifying("Passwords do not match", fields => validate(fields).isDefined)
  )
}
