package io.prediction.data.storage

import io.prediction.data.{ Utils => DataUtils }

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
        val event = fields.get[String]("event")
        val entityType = fields.get[String]("entityType")
        val entityId = fields.get[String]("entityId")
        val targetEntityType = fields.getOpt[String]("targetEntityType")
        val targetEntityId = fields.getOpt[String]("targetEntityId")
        val properties = fields.getOpt[Map[String, JValue]]("properties")
          .getOrElse(Map())
        val eventTime = fields.getOpt[String]("eventTime")
          .map{ s =>
            try {
              DataUtils.stringToDateTime(s)
            } catch {
              case _: Exception =>
                throw new MappingException(s"Fail to extract time")
            }
          }.getOrElse(DateTime.now)
        val tags = fields.getOpt[Seq[String]]("tags").getOrElse(List())
        val appId = fields.get[Int]("appId")
        val predictionKey = fields.getOpt[String]("predictionKey")
        Event(
          event = event,
          entityType = entityType,
          entityId = entityId,
          targetEntityType = targetEntityType,
          targetEntityId = targetEntityId,
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

  def writeJson: PartialFunction[Any, JValue] = serializeToJValue

  def deserializeFromJValue: PartialFunction[JValue, Event] = {
    case jv: JValue => {
      val event = (jv \ "event").extract[String]
      val entityType = (jv \ "entityType").extract[String]
      val entityId = (jv \ "entityId").extract[String]
      val targetEntityType = (jv \ "targetEntityType").extract[Option[String]]
      val targetEntityId = (jv \ "targetEntityId").extract[Option[String]]
      val properties = (jv \ "properties").extract[JObject]
      val eventTime = DataUtils.stringToDateTime(
        (jv \ "eventTime").extract[String])
      val tags = (jv \ "tags").extract[Seq[String]]
      val appId = (jv \ "appId").extract[Int]
      val predictionKey = (jv \ "predictionKey").extract[Option[String]]
      Event(
        event = event,
        entityType = entityType,
        entityId = entityId,
        targetEntityType = targetEntityType,
        targetEntityId = targetEntityId,
        properties = DataMap(properties),
        eventTime = eventTime,
        tags = tags,
        appId = appId,
        predictionKey = predictionKey)
    }
  }

  def serializeToJValue: PartialFunction[Any, JValue] = {
    case d: Event => {
      JObject(
        JField("event", JString(d.event)) ::
        JField("entityType", JString(d.entityType)) ::
        JField("entityId", JString(d.entityId)) ::
        JField("targetEntityType",
          d.targetEntityType.map(JString(_)).getOrElse(JNothing)) ::
        JField("targetEntityId",
          d.targetEntityId.map(JString(_)).getOrElse(JNothing)) ::
        JField("properties", d.properties.toJObject) ::
        JField("eventTime", JString(DataUtils.dateTimeToString(d.eventTime))) ::
        JField("tags", JArray(d.tags.toList.map(JString(_)))) ::
        JField("appId", JInt(d.appId)) ::
        JField("predictionKey",
          d.predictionKey.map(JString(_)).getOrElse(JNothing)) ::
        Nil)
    }
  }

  // for DB usage
  class DBSerializer extends CustomSerializer[Event](format => (
    deserializeFromJValue, serializeToJValue))

  // for API usage
  class APISerializer extends CustomSerializer[Event](format => (
    readJson, writeJson))
}
