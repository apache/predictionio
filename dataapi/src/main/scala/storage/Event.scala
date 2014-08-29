package io.prediction.dataapi.storage

import org.json4s.ext.JodaTimeSerializers
import com.github.nscala_time.time.Imports._
import org.json4s._
import org.json4s.native.Serialization.{ read, write }

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

case class Event(
  val entityId: String,
  val targetEntityId: Option[String],
  val event: String,
  val properties: Map[String, JValue] = Map(),
  //JObject = JObject(List()), // TODO: don't use JObject
  val eventTime: DateTime = DateTime.now, // default to current time
  val tags: Seq[String] = Seq(),
  val appId: Int,
  val predictionKey: Option[String]
) {
  require(!entityId.isEmpty, "entityId must not be empty string.")
  require(targetEntityId.map(!_.isEmpty).getOrElse(true),
    "targetEntityId must not be empty string.")
  require(!event.isEmpty, "event must not be empty.")
}


object EventSerializerFunc {

  implicit val formats = DefaultFormats.lossless
/*
  private def jvalueToAny(j: JValue): Any = {
    j match {
      case JString(s) => s
      case JDouble(num) => num
      case JDecimal(num) => num
      case JInt(num) => num
      case JBool(value) => value
      case JObject(obj) => obj.map{ case (s, jv) => (s, jvalueToAny(jv))}.toMap
      case JArray(arr) => arr.map{jvalueToAny(_)}
      case _ => throw new RuntimeException(s"unknown JValue ${j}")
    }
  }
*/
/*
  private def anyToJValue(a: Any): JValue = {
    a match {
      case s: String => JString(s)
      case num: Double => JDouble(num)
      case num: BigDecimal => JDecimal(num)
      case num: Int => JInt(num)
      case value: Boolean => JBool(value)
      case obj: Map[_, _] => JObject(
        obj.map { case (k,v) => (k.toString, anyToJValue(v))}.toList)
      case arr: Seq[Any] => JArray(arr.toList.map(anyToJValue(_)))
      case _ => throw new RuntimeException(s"unknown Any ${a}")
    }
  }
*/
  private def stringToDateTime(dt: String): DateTime =
    ISODateTimeFormat.dateTimeParser.parseDateTime(dt)

  def jvalueToEvent: PartialFunction[JValue, Event] = {
    case jv: JValue => {
      val entityId = (jv \ "entityId").extract[String]
      val targetEntityId = (jv \ "targetEntityId").extract[Option[String]]
      val event = (jv \ "event").extract[String]
      // work-around the issue that json4s couldn't extract Map[String, Any]
      val properties = (jv \ "properties").extract[JObject].obj.toMap
      //.obj.map { case (k, v) => (k, jvalueToAny(v)) }.toMap
      val eventTime = stringToDateTime((jv \ "eventTime").extract[String])
      val tags = (jv \ "tags").extract[Seq[String]]
      val appId = (jv \ "appId").extract[Int]
      val predictionKey = (jv \ "predictionKey").extract[Option[String]]
      Event(
        entityId = entityId,
        targetEntityId = targetEntityId,
        event = event,
        properties = properties,
        eventTime = eventTime,
        tags = tags,
        appId = appId,
        predictionKey = predictionKey)
    }
  }

  def eventToJValue: PartialFunction[Any, JValue] = {
    case d: Event => {
      JObject(
        JField("entityId", JString(d.entityId)) ::
        JField("targetEntityId",
          d.targetEntityId.map(JString(_)).getOrElse(JNothing)) ::
        JField("event", JString(d.event)) ::
        JField("properties", JObject(d.properties.toList)) ::
          //d.properties.map{ case (k, v) => (k, anyToJValue(v)) }.toList)) ::
        JField("eventTime", JString(d.eventTime.toString)) ::
        JField("tags", JArray(d.tags.toList.map(JString(_)))) ::
        JField("appId", JInt(d.appId)) ::
        JField("predictionKey",
          d.predictionKey.map(JString(_)).getOrElse(JNothing)) ::
        Nil)
    }
  }
}

// json4s serializer
class EventSerializer extends CustomSerializer[Event](format => (
  EventSerializerFunc.jvalueToEvent,
  EventSerializerFunc.eventToJValue))

case class StorageError(val message: String)

trait Events {

  def futureInsert(event: Event): Future[Either[StorageError, String]]

  def futureGet(eventId: String): Future[Either[StorageError, Option[Event]]]

  def futureDelete(eventId: String): Future[Either[StorageError, Boolean]]

  def futureGetByAppId(appId: Int):
    Future[Either[StorageError, Iterator[Event]]]

  def futureDeleteByAppId(appId: Int): Future[Either[StorageError, Unit]]

  // following is blocking
  def insert(event: Event): Either[StorageError, String] = {
    Await.result(futureInsert(event), Duration(5, "seconds"))
  }

  def get(eventId: String): Either[StorageError, Option[Event]] = {
    Await.result(futureGet(eventId), Duration(5, "seconds"))
  }

  def delete(eventId: String): Either[StorageError, Boolean] = {
    Await.result(futureDelete(eventId), Duration(5, "seconds"))
  }

  def getByAppId(appId: Int): Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppId(appId), Duration(5, "seconds"))
  }

  def deleteByAppId(appId: Int): Either[StorageError, Unit] = {
    Await.result(futureDeleteByAppId(appId), Duration(5, "seconds"))
  }

/* old code
  def get(eventId: String): Option[Event]

  def delete(eventId: String): Boolean
*/
}
