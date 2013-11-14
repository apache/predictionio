package controllers

import io.prediction.commons.settings.{AlgoInfo, AlgoInfos, EngineInfo, EngineInfos, Param}

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.format._

object PredictionIOForms {
  def mapOfStringToAny: Mapping[Map[String, Any]] = of[Map[String, Any]]

  implicit def mapOfStringToAnyFormat: Formatter[Map[String, Any]] = new Formatter[Map[String, Any]] {

    override val format = Some(("format.", Nil))

    def bind(key: String, data: Map[String, String]) = {
      data.get(key) map { id =>
        Application.algoInfos.get(id) map { info =>
          val params = info.params
          val allErrorsOrItems: Seq[Either[Seq[FormError], Map[String, Any]]] = params.toSeq map { param =>
            param._2.constraint match {
              case "integer" => Formats.intFormat.bind(param._1, data).right.map(d => Map[String, Any](param._1 -> d))
              case "boolean" => Formats.booleanFormat.bind(param._1, data).right.map(d => Map[String, Any](param._1 -> d))
              case "string" =>  Formats.stringFormat.bind(param._1, data).right.map(d => Map[String, Any](param._1 -> d))
              case "double" =>  Formats.doubleFormat.bind(param._1, data).right.map(d => Map[String, Any](param._1 -> d))
              case _ =>         Left(Seq(FormError(param._1, "error.invalidconstraint", Nil)))
            }
          }
          if (allErrorsOrItems.forall(_.isRight)) {
            Right(((Map[String, Any](key -> id) /: allErrorsOrItems.map(_.right.get))((a, b) => a ++ b)))
          } else {
            Left(allErrorsOrItems.collect { case Left(errors) => errors }.flatten)
          }
        } getOrElse Left(Seq(FormError(key, "error.invalid", Nil)))
      } getOrElse Left(Seq(FormError(key, "error.required", Nil)))
    }

    def unbind(key: String, value: Map[String, Any]) = Map(key -> value(key).toString)
  }
}
