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
import io.prediction.data.webhooks.JsonConnector
import io.prediction.data.storage.Utils

import org.json4s.{Formats, DefaultFormats}
import org.json4s.JValue
import org.json4s.JObject
import org.json4s.JString
import org.json4s.JField
import org.json4s.JNothing

private[prediction] class SegmentIOConnector extends JsonConnector {

  implicit val json4sFormats: Formats = DefaultFormats

  override
  def toEventJson(data: JObject): JObject = {
    // TODO: check segmentio API version
    val common = data.extract[Common]

    common.`type` match {
      case "identify" => toEventJson(
        common = common,
        identify = data.extract[Identify])

      // TODO: support other events
      // TODO: better error handling
      case _ => throw new Exception(s"Cannot process ${data}.")
    }
  }

  def toEventJson(common: Common, identify: Identify): JObject = {

    import org.json4s.JsonDSL._

    val json =
      ("event" -> common.`type`) ~
      ("entityType" -> "user") ~
      ("entityId" -> identify.userId) ~
      ("eventTime" -> common.timestamp) ~
      ("properties" -> (
        ("context" -> common.context) ~
        ("traits" -> identify.traits)
      ))
    json
  }

}

private[prediction] case class Common(
  `type`: String,
  context: Option[JObject],
  timestamp: String
  // TODO: add more fields
)

private[prediction] case class Identify(
  userId: String,
  traits: Option[JObject]
  // TODO: add more fields
)
