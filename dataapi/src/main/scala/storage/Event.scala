package io.prediction.dataapi.storage

//import org.json4s.ext.JodaTimeSerializers
import com.github.nscala_time.time.Imports._
import org.json4s._
import org.json4s.native.Serialization.{ read, write }

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.concurrent.ExecutionContext.Implicits.global // TODO

case class Event(
  val entityId: String,
  val targetEntityId: Option[String] = None,
  val event: String,
  val properties: Map[String, JValue] = Map(),
  //JObject = JObject(List()), // TODO: don't use JObject
  val eventTime: DateTime = DateTime.now, // default to current time
  val tags: Seq[String] = Seq(),
  val appId: Int,
  val predictionKey: Option[String] = None
) {
  require(!entityId.isEmpty, "entityId must not be empty string.")
  require(targetEntityId.map(!_.isEmpty).getOrElse(true),
    "targetEntityId must not be empty string.")
  require(!event.isEmpty, "event must not be empty.")
}

case class DataMapException(msg: String) extends Exception

case class DataMap (
  val fields: Map[String, JValue]
) {
  lazy implicit val formats = DefaultFormats

  def require(name: String) = {
    if (!fields.contains(name))
      throw new DataMapException(s"The field ${name} is required.")
  }

  def get[T: Manifest](name: String): T = {
    require(name)
    fields(name).extract[T]
  }
  // TODO: combine getOpt and get
  def getOpt[T: Manifest](name: String): Option[T] = {
    fields.get(name).map(_.extract[T])
  }

}

object EventJson4sSupport {

  implicit val formats = DefaultFormats

  private def stringToDateTime(dt: String): DateTime =
    ISODateTimeFormat.dateTimeParser.parseDateTime(dt)

  def readJson: PartialFunction[JValue, Event] = {
    case JObject(x) => {
      val fields = new DataMap(x.toMap)
      // use get() if requried in json
      // use getOpt() if not required in json
      try {
        val entityId = fields.get[String]("entityId")
        val targetEntityId = fields.getOpt[String]("targetEntityId")
        val event = fields.get[String]("event")
        val properties = fields.getOpt[Map[String, JValue]]("properties")
          .getOrElse(Map())
        val eventTime = fields.getOpt[String]("eventTime")
          .map{ s =>
            try {
              stringToDateTime(s)
            } catch {
              case _: Exception =>
                throw new MappingException(s"Fail to extract time")
            }
          }.getOrElse(DateTime.now)
        val tags = fields.getOpt[Seq[String]]("tags").getOrElse(List())
        val appId = fields.get[Int]("appId")
        val predictionKey = fields.getOpt[String]("predictionKey")
        Event(
          entityId = entityId,
          targetEntityId = targetEntityId,
          event = event,
          properties = properties,
          eventTime = eventTime,
          appId = appId,
          predictionKey = predictionKey
        )
      } catch {
        case DataMapException(msg) => throw new MappingException(msg)
        case e: Exception => throw new MappingException(e.toString, e)
      }
    }
  }

  def writeJson: PartialFunction[Any, JValue] = serilizeToJValue

  def deserializeFromJValue: PartialFunction[JValue, Event] = {
    case jv: JValue => {
      val entityId = (jv \ "entityId").extract[String]
      val targetEntityId = (jv \ "targetEntityId").extract[Option[String]]
      val event = (jv \ "event").extract[String]
      val properties = (jv \ "properties").extract[JObject].obj.toMap
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

  def serilizeToJValue: PartialFunction[Any, JValue] = {
    case d: Event => {
      JObject(
        JField("event", JString(d.event)) ::
        JField("entityId", JString(d.entityId)) ::
        JField("targetEntityId",
          d.targetEntityId.map(JString(_)).getOrElse(JNothing)) ::
        JField("properties", JObject(d.properties.toList)) ::
        JField("eventTime", JString(d.eventTime.toString)) ::
        JField("tags", JArray(d.tags.toList.map(JString(_)))) ::
        JField("appId", JInt(d.appId)) ::
        JField("predictionKey",
          d.predictionKey.map(JString(_)).getOrElse(JNothing)) ::
        Nil)
    }
  }

  // for DB usage
  class DBSerializer extends CustomSerializer[Event](format => (
    deserializeFromJValue, serilizeToJValue))

  // for API usage
  class APISerializer extends CustomSerializer[Event](format => (
    readJson, writeJson))
}

case class StorageError(val message: String)

trait Events {

  private def notImplemented = Future {
    Left(StorageError("Not implemented."))
  }
  val timeout = Duration(5, "seconds")

  def futureInsert(event: Event): Future[Either[StorageError, String]] =
    notImplemented

  def futureGet(eventId: String): Future[Either[StorageError, Option[Event]]] =
    notImplemented

  def futureDelete(eventId: String): Future[Either[StorageError, Boolean]] =
    notImplemented

  def futureGetByAppId(appId: Int):
    Future[Either[StorageError, Iterator[Event]]] = notImplemented

  /* where t >= start and t < untilTime */
  def futureGetByAppIdAndTime(appId: Int,
    startTime: Option[DateTime], untilTime: Option[DateTime]):
    Future[Either[StorageError, Iterator[Event]]] = notImplemented

  def futureDeleteByAppId(appId: Int): Future[Either[StorageError, Unit]] =
    notImplemented

  // following is blocking
  def insert(event: Event): Either[StorageError, String] = {
    Await.result(futureInsert(event), timeout)
  }

  def get(eventId: String): Either[StorageError, Option[Event]] = {
    Await.result(futureGet(eventId), timeout)
  }

  def delete(eventId: String): Either[StorageError, Boolean] = {
    Await.result(futureDelete(eventId), timeout)
  }

  def getByAppId(appId: Int): Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppId(appId), timeout)
  }

  def getByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime]): Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppIdAndTime(appId, startTime, untilTime), timeout)
  }

  def deleteByAppId(appId: Int): Either[StorageError, Unit] = {
    Await.result(futureDeleteByAppId(appId), timeout)
  }

/* old code
  def get(eventId: String): Option[Event]

  def delete(eventId: String): Boolean
*/
}
