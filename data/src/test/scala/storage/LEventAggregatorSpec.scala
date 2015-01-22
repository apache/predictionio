/** Copyright 2014 TappingStone, Inc.
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

package io.prediction.data.storage

import org.specs2.mutable._

import org.json4s.JObject
import org.json4s.native.JsonMethods.parse

import org.joda.time.DateTime

import scala.util.Random

class LEventAggregatorSpec extends Specification {

  val baseTime = new DateTime(654321)

  def stringToJObject(s: String): JObject = {
    parse(s).asInstanceOf[JObject]
  }

  val e1 = Event(
    event = "$set",
    entityType = "user",
    entityId = "u1",
    properties = DataMap(stringToJObject(
      """{
        "a" : 1,
        "b" : "value2",
        "d" : [1, 2, 3],
      }""")),
    eventTime = baseTime
  )

  val e2 = e1.copy(
    event = "$set",
    properties = DataMap(stringToJObject(
      """{
        "a" : 2
      }""")),
    eventTime = e1.eventTime.plusDays(1)
  )

  val e3 = e1.copy(
    event = "$set",
    properties = DataMap(stringToJObject(
      """{
        "b" : "value4"
      }""")),
    eventTime = e2.eventTime.plusDays(1)
  )

  val e4 = e1.copy(
    event = "$unset",
    properties = DataMap(stringToJObject(
      """{
        "b" : null
      }""")),
    eventTime = e3.eventTime.plusDays(1)
  )

  val e5 = e1.copy(
    event = "$set",
    properties = DataMap(stringToJObject(
      """{
        "e" : "new"
      }""")),
    eventTime = e4.eventTime.plusDays(1)
  )

  val random = new Random(3)

  "LEventAggregator.aggregatePropertiesSingle()" should {
    "aggregate single entity properties correctly" in {
        val events = Vector(e1, e2, e3, e4, e5)
        val eventsIt = random.shuffle(events).toIterator

        val result = LEventAggregator.aggregatePropertiesSingle(eventsIt)
        val expected = DataMap(stringToJObject(
          """{
            "a" : 2,
            "d" : [1, 2, 3],
            "e" : "new"
          }"""
        ))

        result must beEqualTo(Some(expected))
    }

    "aggregate deleted entity correctly" in {
      val e = e1.copy(
        event = "$delete",
        properties = DataMap(),
        eventTime = e5.eventTime.plusDays(1)
      )
      val events = Vector(e1, e2, e3, e4, e5, e)
      val eventsIt = random.shuffle(events).toIterator

      val result = LEventAggregator.aggregatePropertiesSingle(eventsIt)

      result must beEqualTo(None)
    }
  }
}
