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

package io.prediction.data.webhooks

import io.prediction.data.storage.Event
import io.prediction.data.storage.EventJson4sSupport

import org.json4s.JObject
import org.json4s.JString
import org.json4s.JField
import org.json4s.DefaultFormats
import org.json4s.Formats

private[prediction] class WebhooksJsonConverter(val converter: JsonConverter) {

  implicit def json4sFormats: Formats = DefaultFormats +
    new EventJson4sSupport.APISerializer

  // TODO: allow convert to Seq[Event]?
  def convert(data: JObject): Event = {
    // intentionally use EventJson4sSupport.APISerializer to convert
    // from JSON to Event object. Don't allow converter directly create
    // Event object so that the event formation is consistent.
    converter.toEventJson(data).extract[Event]
  }

  def convert(event: Event): JObject = {
    converter.fromEvent(event)
  }
}

/** Converter for Webhooks connection */
private[prediction] abstract class JsonConverter {
  /** Convert from the original JSON to Event API JSON (required)*/
  def toEventJson(data: JObject): JObject

  /** Convert from Event to the original JSON (optional) */
  def fromEvent(event: Event): JObject = {
    JObject()
  }

  /** define this to true if fromEvent() is implemented */
  def isReversible: Boolean = false

}


private[prediction] class WebhooksFormConverter(val converter: FormConverter) {
  implicit def json4sFormats: Formats = DefaultFormats +
    new EventJson4sSupport.APISerializer

  // TODO: allow convert to Seq[Event]?
  def convert(data: Map[String, String]): Event = {
    converter.toEventJson(data).extract[Event]
  }

}

private[prediction] abstract class FormConverter {
  def toEventJson(data: Map[String, String]): JObject
}
