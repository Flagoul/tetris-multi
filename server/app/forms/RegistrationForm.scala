package forms

import play.api.data._
import play.api.data.Forms._


/**
  * Contains data useful for registering a user
  */
object RegistrationForm {

  /**
    * Class to encapsulate registration data
    *
    * @param name of the user
    * @param password of the user
    * @param password_confirmation the password again
    */
  case class UserRegistrationData(name: String, password: String, password_confirmation: String)

  /**
    * Validate UserRegistrationData for passwords to match.
    *
    * @param data to validate
    *
    * @return the data if it matches or None
    */
  def validate(data: UserRegistrationData): Option[UserRegistrationData] = {
    data.password match {
      case data.password_confirmation => Some(data)
      case _ => None
    }
  }

  /**
    * Form for registration
    */
  val form = Form(
    mapping(
      "username" -> nonEmptyText(maxLength = 255),
      "password" -> nonEmptyText,
      "password_confirmation" -> nonEmptyText
    )(UserRegistrationData.apply)(UserRegistrationData.unapply)
      .verifying("Passwords do not match", fields => validate(fields).isDefined)
  )
}
