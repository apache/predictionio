package io.prediction.data.storage

import io.prediction.data.{ Utils => DataUtils }

import org.json4s._
import org.json4s.native.Serialization.{ read, write }

import org.joda.time.DateTime

object DateTimeJson4sSupport {

  implicit val formats = DefaultFormats

  def serializeToJValue: PartialFunction[Any, JValue] = {
    case d: DateTime => JString(DataUtils.dateTimeToString(d))
  }

  def deserializeFromJValue: PartialFunction[JValue, DateTime] = {
    case jv: JValue => DataUtils.stringToDateTime(jv.extract[String])
  }

  class serializer extends CustomSerializer[DateTime](format => (
    deserializeFromJValue, serializeToJValue))

}
