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

import org.specs2._
import org.specs2.specification.Step

import org.json4s.JObject
import org.json4s.native.JsonMethods.parse

import org.joda.time.DateTime

class EventsSpec extends Specification {
  def is = s2"""

  PredictionIO Storage Events Specification

    Events can be implemented by:
    - HBEvents ${hbEvents}

  """

  def hbEvents = s2"""

    HBEvents should" ^
    - behave like any Events implementation ${events(hbDO)}
    - (table cleanup) ${Step(StorageTestUtils.dropHBaseNamespace(dbName))}

  """

  def events(eventClient: LEvents) = s2"""

    inserting and getting 3 Events ${insertTest(eventClient)}

  """

  val dbName = "test_pio_storage_events_" + hashCode
  def hbDO = Storage.getDataObject[LEvents](
    StorageTestUtils.hbaseSourceName,
    dbName
  )

  def insertTest(eventClient: LEvents) = {

    val appId = 1

    eventClient.init(appId)

    val listOfEvents = List(
      Event(
        event = "my_event",
        entityType = "my_entity_type",
        entityId = "my_entity_id",
        targetEntityType = Some("my_target_entity_type"),
        targetEntityId = Some("my_target_entity_id"),
        properties = DataMap(parse(
          """{
            "prop1" : 1,
            "prop2" : "value2",
            "prop3" : [1, 2, 3],
            "prop4" : true,
            "prop5" : ["a", "b", "c"],
            "prop6" : 4.56
          }"""
          ).asInstanceOf[JObject]),
        eventTime = DateTime.now,
        prId = Some("my_prid")
      ),
      Event(
        event = "my_event2",
        entityType = "my_entity_type2",
        entityId = "my_entity_id2"
      ),
      Event(
        event = "my_event3",
        entityType = "my_entity_type",
        entityId = "my_entity_id",
        targetEntityType = Some("my_target_entity_type"),
        targetEntityId = Some("my_target_entity_id"),
        properties = DataMap(parse(
          """{
            "propA" : 1.2345,
            "propB" : "valueB",
          }"""
          ).asInstanceOf[JObject]),
        prId = Some("my_prid")
      )
    )

    val insertResp = listOfEvents.map { eventClient.insert(_, appId) }

    val insertedEventId: List[String] = insertResp.map { resp =>
      resp.right.get
    }

    val insertedEvent: List[Option[Event]] = listOfEvents.zip(insertedEventId)
      .map { case (e, id) => Some(e.copy(eventId = Some(id))) }

    val getResp = insertedEventId.map { id => eventClient.get(id, appId) }

    val getEvents = getResp.map { resp => resp.right.get }

    insertedEvent must containTheSameElementsAs(getEvents)
  }
}
