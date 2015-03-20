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


package io.prediction.data.webhooks.mailchimp

import io.prediction.data.webhooks.FormConnector
import io.prediction.data.storage.EventValidation
import io.prediction.data.Utils

import org.json4s.DefaultFormats
import org.json4s.JObject
import org.json4s.JNothing

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

private[prediction] object MailChimpConnector extends FormConnector {

  override
  def toEventJson(data: Map[String, String]): JObject = {

    // TODO: handle if the key "type" does not exist
    val json = data("type") match {
      case "subscribe" => subscribeToEventJson(data)
      // TODO: support other events
      // TODO: better error handling
      case _ => throw new Exception(s"Cannot convert ${data} to event JSON")
    }
    json
  }


  val mailChimpDateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    .withZone(EventValidation.defaultTimeZone)

  def parseMailChimpDateTime(s: String): DateTime = {
    mailChimpDateTimeFormat.parseDateTime(s)
  }

  def subscribeToEventJson(data: Map[String, String]): JObject = {

    import org.json4s.JsonDSL._

    /*
    "type": "subscribe",
    "fired_at": "2009-03-26 21:35:57",
    "data[id]": "8a25ff1d98",
    "data[list_id]": "a6b5da1054",
    "data[email]": "api@mailchimp.com",
    "data[email_type]": "html",
    "data[merges][EMAIL]": "api@mailchimp.com",
    "data[merges][FNAME]": "MailChimp",
    "data[merges][LNAME]": "API",
    "data[merges][INTERESTS]": "Group1,Group2",
    "data[ip_opt]": "10.20.10.30",
    "data[ip_signup]": "10.20.10.30"
    */

    // convert to ISO8601 format
    val eventTime = Utils.dateTimeToString(parseMailChimpDateTime(data("fired_at")))

    // TODO: handle optional fields
    val json =
      ("event" -> "subscribe") ~
      ("entityType" -> "user") ~
      ("entityId" -> data("data[id]")) ~
      ("targetEntityType" -> "list") ~
      ("targetEntityId" -> data("data[list_id]")) ~
      ("eventTime" -> eventTime) ~
      ("properties" -> (
        ("email" -> data("data[email]")) ~
        ("email_type" -> data("data[email_type]")) ~
        ("merges" -> (
          ("EMAIL" -> data("data[merges][EMAIL]")) ~
          ("FNAME" -> data("data[merges][FNAME]"))) ~
          ("LNAME" -> data("data[merges][LNAME]")) ~
          ("INTERESTS" -> data("data[merges][INTERESTS]"))
        )) ~
        ("ip_opt" -> data("data[ip_opt]")) ~
        ("ip_signup" -> data("data[ip_signup]")
      ))

    json

  }

}
