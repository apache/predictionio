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

package io.prediction.data.storage

import org.joda.time.DateTime

trait TestEvents {

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

  val u1LastTime = u1BaseTime.plusDays(4)
  val u1 = """{"a": 2, "d": [1, 2, 3], "e": "new"}"""

  // delete event for u1
  val u1ed = u1e1.copy(
    event = "$delete",
    properties = DataMap(),
    eventTime = u1BaseTime.plusDays(5)
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

  val u2LastTime = u2BaseTime.plusDays(2)
  val u2 = """{"b": "value9", "d": [7, 5, 6], "g": "new11"}"""

}
