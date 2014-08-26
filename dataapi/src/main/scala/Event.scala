package io.prediction.dataapi

import org.json4s.ext.JodaTimeSerializers
import com.github.nscala_time.time.Imports._
import org.json4s._
import org.json4s.native.Serialization.{ read, write }

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class Event(
  val entityId: String,
  val targetEntityId: Option[String],
  val event: String,
  val properties: JObject = JObject(List()), //Map[String, Any], // TODO: don't use JObject
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

// json4s serializer
/* TODO: No need */
class EventSeriliazer extends CustomSerializer[Event](format => (
  {
    case jv: JValue => {
      implicit val formats = DefaultFormats.lossless ++ JodaTimeSerializers.all
      val entityId = (jv \ "entityId").extract[String]
      val targetEntityId = (jv \ "targetEntityId").extract[Option[String]]
      val event = (jv \ "event").extract[String]
      val properties = (jv \ "properties").extract[JObject]
      val eventTime = (jv \ "eventTime").extract[DateTime]
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
  },
  {
    case d: Event => {
      implicit val formats = DefaultFormats.lossless ++ JodaTimeSerializers.all
      JObject(
        JField("entityId", JString(d.entityId)) ::
        JField("targetEntityId",
          d.targetEntityId.map(JString(_)).getOrElse(JNothing)) ::
        JField("event", JString(d.event)) ::
        JField("properties", d.properties) ::
        JField("eventTime", JString(write(d.eventTime))) ::
        JField("tags", JArray(d.tags.toList.map(JString(_)))) ::
        JField("appId", JInt(d.appId)) ::
        JField("predictionKey",
          d.predictionKey.map(JString(_)).getOrElse(JNothing)) ::
        Nil)
      }
  }
))


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
