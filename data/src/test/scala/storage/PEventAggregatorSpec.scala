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

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import org.joda.time.DateTime

class PEventAggregatorSpec extends Specification {

  System.clearProperty("spark.driver.port")
  System.clearProperty("spark.hostPort")
  val sc = new SparkContext("local[4]", "PEventAggregatorSpec test")

  val u1BaseTime = new DateTime(654321)
  val u2BaseTime = new DateTime(6543210)

  // u1 events
  val u1e1 = Event(
    event = "$set",
    entityType = "user",
    entityId = "u1",
    properties = DataMap(
      """{
        "a" : 1,
        "b" : "value2",
        "d" : [1, 2, 3],
      }"""),
    eventTime = u1BaseTime
  )

  val u1e2 = u1e1.copy(
    event = "$set",
    properties = DataMap("""{"a" : 2}"""),
    eventTime = u1BaseTime.plusDays(1)
  )

  val u1e3 = u1e1.copy(
    event = "$set",
    properties = DataMap("""{"b" : "value4"}"""),
    eventTime = u1BaseTime.plusDays(2)
  )

  val u1e4 = u1e1.copy(
    event = "$unset",
    properties = DataMap("""{"b" : null}"""),
    eventTime = u1BaseTime.plusDays(3)
  )

  val u1e5 = u1e1.copy(
    event = "$set",
    properties = DataMap("""{"e" : "new"}"""),
    eventTime = u1BaseTime.plusDays(4)
  )

  // u2 events
  val u2e1 = Event(
    event = "$set",
    entityType = "user",
    entityId = "u2",
    properties = DataMap(
      """{
        "a" : 21,
        "b" : "value12",
        "d" : [7, 5, 6],
      }"""),
    eventTime = u2BaseTime
  )

  val u2e2 = u2e1.copy(
    event = "$unset",
    properties = DataMap("""{"a" : null}"""),
    eventTime = u2BaseTime.plusDays(1)
  )

  val u2e3 = u2e1.copy(
    event = "$set",
    properties = DataMap("""{"b" : "value9", "g": "new11"}"""),
    eventTime = u2BaseTime.plusDays(2)
  )


  "PEventAggregator" should {

    val u1 = """{"a": 2, "d": [1, 2, 3], "e": "new"}"""
    val u2 = """{"b": "value9", "d": [7, 5, 6], "g": "new11"}"""

    "aggregate properties as DataMap and PropertyMap correctly" in {
      val events = sc.parallelize(Seq(
        u1e5, u2e2, u1e3, u1e1, u2e3, u2e1, u1e4, u1e2))

      val users = PEventAggregator.aggregateProperties(events)

      val userMap = users.collectAsMap.toMap
      val expectedDM = Map(
        "u1" -> DataMap(u1),
        "u2" -> DataMap(u2)
      )

      val expectedPM = Map(
        "u1" -> PropertyMap(u1, u1BaseTime, u1BaseTime.plusDays(4)),
        "u2" -> PropertyMap(u2, u2BaseTime, u2BaseTime.plusDays(2))
      )

      userMap must beEqualTo(expectedDM)
      userMap must beEqualTo(expectedPM)
    }

    "aggregate deleted entity correctly" in {
      val ed = u1e1.copy(
        event = "$delete",
        properties = DataMap(),
        eventTime = u1BaseTime.plusDays(5)
      )

      // put the delete event in middle
      val events = sc.parallelize(Seq(
        u1e5, u2e2, u1e3, ed, u1e1, u2e3, u2e1, u1e4, u1e2))

      val users = PEventAggregator.aggregateProperties(events)

      val userMap = users.collectAsMap.toMap
      val expectedPM = Map(
        "u2" -> PropertyMap(u2, u2BaseTime, u2BaseTime.plusDays(2))
      )

      userMap must beEqualTo(expectedPM)
    }

  }

  step(sc.stop())
}
