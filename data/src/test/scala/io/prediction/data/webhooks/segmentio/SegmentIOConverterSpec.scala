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

import org.specs2.mutable._

import org.json4s.JObject
import org.json4s.Formats
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization.write

class SegmentIOConnectorSpec extends Specification {

  // TOOD: test other events
  // TODO: test different optional fields

  implicit val formats = DefaultFormats

  val connector = new SegmentIOConnector()

  "SegmentIOConnector" should {

    "convert identify to event JSON" in {
      // simple format
      val identify = parse("""
        {
          "version"   : 1,
          "type"      : "identify",
          "userId"    : "019mr8mf4r",
          "traits"    : {
              "email"            : "achilles@segment.com",
              "name"             : "Achilles",
              "subscriptionPlan" : "Premium",
              "friendCount"      : 29
          },
          "timestamp" : "2012-12-02T00:30:08.276Z"
        }
      """).asInstanceOf[JObject]

      // write and parse back to discard any JNothing field
      val event = parse(write(connector.toEventJson(identify))).asInstanceOf[JObject]

      val expected = parse("""
        {
          "event" : "identify",
          "entityType": "user",
          "entityId" : "019mr8mf4r",
          "properties" : {
            "traits" : {
              "email"            : "achilles@segment.com",
              "name"             : "Achilles",
              "subscriptionPlan" : "Premium",
              "friendCount"      : 29
            }
          },
          "eventTime" : "2012-12-02T00:30:08.276Z"
        }
      """).asInstanceOf[JObject]

      event.obj must containTheSameElementsAs(expected.obj)
    }

  }

}
