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

class LEventAggregatorSpec extends Specification {

  val baseTime = new DateTime(654321)

  val e1 = Event(
    event = "$set",
    entityType = "user",
    entityId = "u1",
    properties = DataMap(
      """{
        "a" : 1,
        "b" : "value2",
        "d" : [1, 2, 3],
      }"""),
    eventTime = baseTime
  )

  val e2 = e1.copy(
    event = "$set",
    properties = DataMap("""{"a" : 2}"""),
    eventTime = baseTime.plusDays(1)
  )

  val e3 = e1.copy(
    event = "$set",
    properties = DataMap("""{"b" : "value4"}"""),
    eventTime = baseTime.plusDays(2)
  )

  val e4 = e1.copy(
    event = "$unset",
    properties = DataMap("""{"b" : null}"""),
    eventTime = baseTime.plusDays(3)
  )

  val e5 = e1.copy(
    event = "$set",
    properties = DataMap("""{"e" : "new"}"""),
    eventTime = baseTime.plusDays(4)
  )

  "LEventAggregator.aggregatePropertiesSingle()" should {

    "aggregate single entity properties as DataMap correctly" in {
        val events = Vector(e5, e3, e1, e4, e2)
        val eventsIt = events.toIterator

        val result: Option[DataMap] = LEventAggregator
          .aggregatePropertiesSingle(eventsIt)
        val expected = DataMap(
          """{
            "a" : 2,
            "d" : [1, 2, 3],
            "e" : "new"
          }"""
        )

        result must beEqualTo(Some(expected))
    }

    "aggregate single entity properties as PropertyMap correctly" in {
        val events = Vector(e5, e3, e1, e4, e2)
        val eventsIt = events.toIterator

        val result: Option[PropertyMap] = LEventAggregator
          .aggregatePropertiesSingle(eventsIt)
        val expected = PropertyMap(
          """{
            "a" : 2,
            "d" : [1, 2, 3],
            "e" : "new"
          }""",
          firstUpdated = baseTime,
          lastUpdated = baseTime.plusDays(4)
        )

        result must beEqualTo(Some(expected))
    }

    "aggregate deleted entity correctly" in {
      val ed = e1.copy(
        event = "$delete",
        properties = DataMap(),
        eventTime = baseTime.plusDays(5)
      )
      // put the delete event in the middle
      val events = Vector(e4, e2, ed, e3, e1, e5)
      val eventsIt = events.toIterator

      val result = LEventAggregator.aggregatePropertiesSingle(eventsIt)

      result must beEqualTo(None)
    }
  }
}
