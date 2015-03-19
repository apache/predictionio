/** Copyright 2015 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.data.webhooks.segmentio

import io.prediction.data.storage.Event
import io.prediction.data.webhooks.JsonConverter
import io.prediction.data.storage.Utils

import org.json4s.{Formats, DefaultFormats}
import org.json4s.JValue
import org.json4s.JObject
import org.json4s.JString
import org.json4s.JField
import org.json4s.JNothing

private[prediction] case class Common(
  `type`: String,
  context: Option[JObject],
  timestamp: String
  // TODO: add more fields
) {

  def toJFields: List[JField] = {
    JField("type", JString(`type`)) ::
    JField("context", context.getOrElse(JNothing)) ::
    JField("timestamp", JString(timestamp)) ::
    Nil
  }
}

private[prediction] case class Identify(
  userId: String,
  traits: Option[JObject]
  // TODO: add more fields
) {

  def toJFields: List[JField] = {
    JField("userId", JString(userId)) ::
    JField("traits", traits.getOrElse(JNothing)) ::
    Nil
  }

}

object IdentifyConverter {

  def toEventJson(common: Common, identify: Identify): JObject = {
    // optional
    val propertiesList =
      JField("context", common.context.getOrElse(JNothing)) ::
      JField("traits", identify.traits.getOrElse(JNothing)) ::
      Nil

    JObject(
      JField("event", JString(common.`type`)) ::
      JField("entityType", JString("user")) ::
      JField("entityId", JString(identify.userId)) ::
      JField("eventTime", JString(common.timestamp)) ::
      JField("properties", JObject(propertiesList)) ::
      Nil
    )
  }

  def fromEvent(event: Event): JObject = {
    val common = Common(
      `type` = event.event,
      context = event.properties.getOpt[JObject]("context"),
      timestamp = event.eventTime.toString
    )

    val identify = Identify(
      userId = event.entityId,
      traits = event.properties.getOpt[JObject]("traits")
    )

    JObject(common.toJFields ++ identify.toJFields)
  }
}



private[prediction] class SegmentIOConverter extends JsonConverter {

  implicit def json4sFormats: Formats = DefaultFormats

  override
  def toEventJson(data: JObject): JObject = {
    // TODO: check segmentio API version
    val common = data.extract[Common]

    common.`type` match {
      case "identify" => IdentifyConverter.toEventJson(
        common, data.extract[Identify])
      // TODO: better error handling
      case _ => throw new Exception(s"Cannot process ${data}.")
    }
  }

  override
  def fromEvent(event: Event): JObject = {
    event.event match {
      case "identify" => IdentifyConverter.fromEvent(event)
      case _ => throw new Exception(s"Cannot convert from ${event}.")
    }
  }

  override
  def isReversible = true

}
