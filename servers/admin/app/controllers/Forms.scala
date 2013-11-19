package controllers

import io.prediction.commons.settings.{AlgoInfo, AlgoInfos, EngineInfo, EngineInfos, Param}

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.format._

object Forms {
  def mapOfStringToAny: Mapping[Map[String, Any]] = of[Map[String, Any]]

  private val infotypeKey = "infotype"
  private val scopeKey = "scope"

  private def scoped(scope: Option[String], scopes: Option[Set[String]], result: Either[Seq[FormError], Map[String, Any]]): Either[Seq[FormError], Map[String, Any]] = {
    val resultOrEmpty = result match {
      case Right(s) => Right(s)
      case Left(s)  => Right(Map[String, Any]())
    }

    (scope, scopes) match {
      case (Some(s), Some(ss)) => if (ss(s)) result else resultOrEmpty
      case _ => resultOrEmpty
    }
  }

  implicit def mapOfStringToAnyFormat: Formatter[Map[String, Any]] = new Formatter[Map[String, Any]] {
    def bind(key: String, data: Map[String, String]) = {

      data.get(infotypeKey) map { infotype =>
        data.get(key) map { id =>
          val params = infotype match {
            case "algo"   => Some(Application.algoInfos.get(id) map { _.params })
            case "engine" => Some(Application.engineInfos.get(id) map { _.defaultsettings })
            case _        => None
          }

          params map { someParams =>
            someParams map { params =>
              val allErrorsOrItems: Seq[Either[Seq[FormError], Map[String, Any]]] = params.toSeq map { param =>
                param._2.constraint match {
                  case "integer" => scoped(data.get(scopeKey), param._2.scopes, Formats.intFormat.bind(param._1, data).right.map(d => Map[String, Any](param._1 -> d)))
                  case "boolean" => scoped(data.get(scopeKey), param._2.scopes, Formats.booleanFormat.bind(param._1, data).right.map(d => Map[String, Any](param._1 -> d)))
                  case "string" =>  scoped(data.get(scopeKey), param._2.scopes, Formats.stringFormat.bind(param._1, data).right.map(d => Map[String, Any](param._1 -> d)))
                  case "double" =>  scoped(data.get(scopeKey), param._2.scopes, Formats.doubleFormat.bind(param._1, data).right.map(d => Map[String, Any](param._1 -> d)))
                  case _ =>         scoped(data.get(scopeKey), param._2.scopes, Left(Seq(FormError(param._1, "error.invalidconstraint", Nil))))
                }
              }
              if (allErrorsOrItems.forall(_.isRight)) {
                Right(((Map[String, Any](key -> id) /: allErrorsOrItems.map(_.right.get))((a, b) => a ++ b)))
              } else {
                Left(allErrorsOrItems.collect { case Left(errors) => errors }.flatten)
              }
            } getOrElse Left(Seq(FormError(key, "error.invalid", Nil)))
          } getOrElse Left(Seq(FormError(infotypeKey, "error.invalid", Nil)))
        } getOrElse Left(Seq(FormError(key, "error.required", Nil)))
      } getOrElse Left(Seq(FormError(infotypeKey, "error.required", Nil)))
    }

    def unbind(key: String, value: Map[String, Any]) = Map(key -> value(key).toString)
  }
}
