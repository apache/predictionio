package controllers

import io.prediction.commons.settings.{ AlgoInfo, AlgoInfos, EngineInfo, EngineInfos, Param }

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation._
import play.api.data.format._

object Forms {
  def mapOfStringToAny: Mapping[Map[String, Any]] = of[Map[String, Any]]
  def seqOfMapOfStringToAny: Mapping[Seq[Map[String, Any]]] = of[Seq[Map[String, Any]]]

  private val infotypeKey = "infotype"
  private val scopeKey = "scope"

  private def scoped(scope: Option[String], scopes: Option[Set[String]], result: Either[Seq[FormError], Map[String, Any]]): Either[Seq[FormError], Map[String, Any]] = {
    val resultOrEmpty = result match {
      case Right(s) => Right(s)
      case Left(s) => Right(Map[String, Any]())
    }

    (scope, scopes) match {
      case (Some(s), Some(ss)) => if (ss(s)) result else resultOrEmpty
      case _ => result
    }
  }

  implicit def mapOfStringToAnyFormat: Formatter[Map[String, Any]] = new Formatter[Map[String, Any]] {
    def bind(key: String, data: Map[String, String]) = {
      data.get(infotypeKey) map { infotype =>
        data.get(key) map { id =>
          bindParams(key, infotypeKey, infotype, id, data, None)
        } getOrElse Left(Seq(FormError(key, "error.required", Nil)))
      } getOrElse Left(Seq(FormError(infotypeKey, "error.required", Nil)))
    }

    def unbind(key: String, value: Map[String, Any]) = Map(key -> value(key).toString)
  }

  implicit def seqOfMapOfStringToAnyFormat: Formatter[Seq[Map[String, Any]]] = new Formatter[Seq[Map[String, Any]]] {
    def bind(key: String, data: Map[String, String]) = {
      // Get available indexes of infotypeKey first.
      val results = RepeatedMapping.indexes(infotypeKey, data) map { i =>
        val infotype = data.get(infotypeKey + "[" + i + "]").get
        data.get(key + "[" + i + "]") map { id =>
          bindParams(key, infotypeKey, infotype, id, data, Some(i))
        } getOrElse Left(Seq(FormError(key + "[" + i + "]", "error.required", Nil)))
      }

      if (results.forall(_.isRight)) {
        Right(results.map(_.right.get))
      } else {
        Left(results.collect { case Left(errors) => errors }.flatten)
      }
    }

    def unbind(key: String, value: Seq[Map[String, Any]]) = {
      (for ((v, i) <- value.zipWithIndex) yield Map(key + "[" + i + "]" -> v(key).toString)).reduce(_ ++ _)
    }
  }

  private def bindParams(key: String, infotypeKey: String, infotype: String, infoid: String, data: Map[String, String], index: Option[Int]): Either[Seq[FormError], Map[String, Any]] = {
    val indexedKey = index map { key + "[" + _ + "]" } getOrElse key
    val indexedInfotypeKey = index map { infotypeKey + "[" + _ + "]" } getOrElse infotypeKey
    val infoParams = infotype match {
      case "algo" => Some(Application.algoInfos.get(infoid) map { _.params })
      case "engine" => Some(Application.engineInfos.get(infoid) map { _.params })
      case "offlineevalmetric" => Some(Application.offlineEvalMetricInfos.get(infoid) map { _.params })
      case "offlineevalsplitter" => Some(Application.offlineEvalSplitterInfos.get(infoid) map { _.params })
      case _ => None
    }

    infoParams map { someParams =>
      someParams map { params =>
        val allErrorsOrItems: Seq[Either[Seq[FormError], Map[String, Any]]] = params.toSeq map { param =>
          val indexedParamKey = index map { param._1 + "[" + _ + "]" } getOrElse param._1
          param._2.constraint.paramtype match {
            case "integer" => scoped(data.get(scopeKey), param._2.scopes, Formats.intFormat.bind(indexedParamKey, data).right.map(d => Map[String, Any](param._1 -> d)))
            case "long" => scoped(data.get(scopeKey), param._2.scopes, Formats.longFormat.bind(indexedParamKey, data).right.map(d => Map[String, Any](param._1 -> d)))
            case "boolean" => scoped(data.get(scopeKey), param._2.scopes, Formats.booleanFormat.bind(indexedParamKey, data).right.map(d => Map[String, Any](param._1 -> d)))
            case "string" => scoped(data.get(scopeKey), param._2.scopes, Formats.stringFormat.bind(indexedParamKey, data).right.map(d => Map[String, Any](param._1 -> d)))
            case "double" => scoped(data.get(scopeKey), param._2.scopes, Formats.doubleFormat.bind(indexedParamKey, data).right.map(d => Map[String, Any](param._1 -> d)))
            case _ => scoped(data.get(scopeKey), param._2.scopes, Left(Seq(FormError(indexedParamKey, "error.invalidconstraint", Nil))))
          }
        }
        if (allErrorsOrItems.forall(_.isRight)) {
          Right(((Map[String, Any](key -> infoid, infotypeKey -> infotype) /: allErrorsOrItems.map(_.right.get))((a, b) => a ++ b)))
        } else {
          Left(allErrorsOrItems.collect { case Left(errors) => errors }.flatten)
        }
      } getOrElse Left(Seq(FormError(indexedKey, "error.invalid", Nil)))
    } getOrElse Left(Seq(FormError(indexedInfotypeKey, "error.invalid", Nil)))
  }
}
