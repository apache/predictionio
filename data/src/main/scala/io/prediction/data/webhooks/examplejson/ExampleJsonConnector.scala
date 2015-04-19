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

package io.prediction.data.webhooks.examplejson

import io.prediction.data.webhooks.JsonConnector

import org.json4s.Formats
import org.json4s.DefaultFormats
import org.json4s.JObject

import scala.util.{Failure, Success, Try}
import scalaz.\/
import scalaz.Scalaz._

/** Example JsonConnector with following types of webhooks JSON input:
  *
  * UserAction
  *
  * {
  *   "type": "userAction"
  *   "userId": "as34smg4",
  *   "event": "do_something",
  *   "context": {
  *     "ip": "24.5.68.47",
  *     "prop1": 2.345,
  *     "prop2": "value1"
  *   },
  *   "anotherProperty1": 100,
  *   "anotherProperty2": "optional1",
  *   "timestamp": "2015-01-02T00:30:12.984Z"
  * }
  *
  * UserActionItem
  *
  * {
  *   "type": "userActionItem"
  *   "userId": "as34smg4",
  *   "event": "do_something_on",
  *   "itemId": "kfjd312bc",
  *   "context": {
  *     "ip": "1.23.4.56",
  *     "prop1": 2.345,
  *     "prop2": "value1"
  *   },
  *   "anotherPropertyA": 4.567,
  *   "anotherPropertyB": false,
  *   "timestamp": "2015-01-15T04:20:23.567Z"
  * }
  */
private[prediction] object ExampleJsonConnector extends JsonConnector {

  implicit val json4sFormats: Formats = DefaultFormats

  override def toEventJson(data: JObject): String \/ JObject = {
    (Try(data.extract[Common]) match {
      case Success(common) ⇒ common.right
      case Failure(e) ⇒
        s"Cannot extract Common field from ${data }. ${e.getMessage() }".left
    }).flatMap { common ⇒
      common.`type` match {
        case "userAction" =>
          toEventJson(common = common, userAction = data.extract[UserAction])
        case "userActionItem" =>
          toEventJson(common = common, userActionItem = data.extract[UserActionItem])
        case x: String =>
          s"Cannot convert unknown type '${x }' to Event JSON.".left
      }
    }
  }

  def toEventJson(common: Common, userAction: UserAction): String \/ JObject = {
    import org.json4s.JsonDSL._

    // map to EventAPI JSON
    val json =
      ("event" -> userAction.event) ~
      ("entityType" -> "user") ~
      ("entityId" -> userAction.userId) ~
      ("eventTime" -> userAction.timestamp) ~
      ("properties" -> (
        ("context" -> userAction.context) ~
        ("anotherProperty1" -> userAction.anotherProperty1) ~
        ("anotherProperty2" -> userAction.anotherProperty2)
      ))
    json.right
  }

  def toEventJson(
    common: Common,
    userActionItem: UserActionItem
  ): String \/ JObject = {
    import org.json4s.JsonDSL._

    // map to EventAPI JSON
    val json =
      ("event" -> userActionItem.event) ~
      ("entityType" -> "user") ~
      ("entityId" -> userActionItem.userId) ~
      ("targetEntityType" -> "item") ~
      ("targetEntityId" -> userActionItem.itemId) ~
      ("eventTime" -> userActionItem.timestamp) ~
      ("properties" -> (
        ("context" -> userActionItem.context) ~
        ("anotherPropertyA" -> userActionItem.anotherPropertyA) ~
        ("anotherPropertyB" -> userActionItem.anotherPropertyB)
      ))
    json.right
  }

  // Common required fields
  case class Common(
    `type`: String
  )

  // User Actions fields
  case class UserAction (
    userId: String,
    event: String,
    context: Option[JObject],
    anotherProperty1: Int,
    anotherProperty2: Option[String],
    timestamp: String
  )

  // UserActionItem fields
  case class UserActionItem (
    userId: String,
    event: String,
    itemId: String,
    context: JObject,
    anotherPropertyA: Option[Double],
    anotherPropertyB: Option[Boolean],
    timestamp: String
  )

}
