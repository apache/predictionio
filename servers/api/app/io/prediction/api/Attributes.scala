package io.prediction.api

import play.api.data._

class Attributes[T](mandatory: Mapping[T], remove: Set[String]) {
  val form = Form(mandatory)

  def bindFromRequestAndFold[R](hasErrors: Form[T] => R, success: (T, Map[String, String]) => R)(implicit request: play.api.mvc.Request[_]): R = {
    val requestData = (request.body match {
      case body: play.api.mvc.AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
      case _ => Map.empty[String, Seq[String]]
    })
    val formWithData = form.bindFromRequest
    val extras = formWithData.data -- remove
    formWithData.value.map(success(_, extras)).getOrElse(hasErrors(formWithData))
  }
}

object Attributes {
  def apply[T](mandatory: Mapping[T], remove: Set[String]): Attributes[T] = new Attributes[T](mandatory, remove)
}
