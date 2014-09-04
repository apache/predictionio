package io.prediction.data.storage

import io.prediction.data.Utils

import org.json4s._
import org.json4s.native.Serialization.{ read, write }

import org.joda.time.DateTime

object EventJson4sSupport {

  // NOTE: don't use json4s to serialize/deserialize joda DateTime
  // because it has some issue with timezone (as of version 3.2.10)
  implicit val formats = DefaultFormats

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
              Utils.stringToDateTime(s)
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
          properties = DataMap(properties),
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
      val properties = (jv \ "properties").extract[JObject]
      val eventTime = Utils.stringToDateTime((jv \ "eventTime").extract[String])
      val tags = (jv \ "tags").extract[Seq[String]]
      val appId = (jv \ "appId").extract[Int]
      val predictionKey = (jv \ "predictionKey").extract[Option[String]]
      Event(
        entityId = entityId,
        targetEntityId = targetEntityId,
        event = event,
        properties = DataMap(properties),
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
        JField("properties", d.properties.toJObject) ::
        JField("eventTime", JString(Utils.dateTimeToString(d.eventTime))) ::
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
