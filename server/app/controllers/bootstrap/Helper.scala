package controllers.bootstrap

import views.html.helper.FieldConstructor

/**
  * Helper to get access to bootstrap's fields defined in `views.html.bootstrap.field`.
  *
  * For this to be available, it must be imported in the template that requires it.
  */
object Helper {
  implicit val myFields: FieldConstructor = FieldConstructor(views.html.bootstrap.field.f)
}
