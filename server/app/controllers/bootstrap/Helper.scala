package controllers.bootstrap

import views.html.helper.FieldConstructor

object Helper {
  implicit val myFields = FieldConstructor(views.html.bootstrap.field.f)
}
