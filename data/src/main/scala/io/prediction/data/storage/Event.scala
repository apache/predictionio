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
import org.joda.time.DateTimeZone

/** Each event in the Event Store can be represented by fields in this case
  * class.
  *
  * @param eventId Unique ID of this event.
  * @param event Name of this event.
  * @param entityType Type of the entity associated with this event.
  * @param entityId ID of the entity associated with this event.
  * @param targetEntityType Type of the target entity associated with this
  *                         event.
  * @param targetEntityId ID of the target entity associated with this event.
  * @param properties Properties associated with this event.
  * @param eventTime Time of the happening of this event.
  * @param tags Tags of this event.
  * @param prId PredictedResultId of this event.
  * @param creationTime Time of creation in the system of this event.
  */
case class Event(
  val eventId: Option[String] = None,
  val event: String,
  val entityType: String,
  val entityId: String,
  val targetEntityType: Option[String] = None,
  val targetEntityId: Option[String] = None,
  val properties: DataMap = DataMap(), // default empty
  val eventTime: DateTime = DateTime.now,
  val tags: Seq[String] = Nil,
  val prId: Option[String] = None,
  val creationTime: DateTime = DateTime.now
) {
  override def toString(): String = {
    s"Event(id=$eventId,event=$event,eType=$entityType,eId=$entityId," +
    s"tType=$targetEntityType,tId=$targetEntityId,p=$properties,t=$eventTime," +
    s"tags=$tags,pKey=$prId,ct=$creationTime)"
  }
}

private[prediction] object EventValidation {

  val defaultTimeZone = DateTimeZone.UTC

  // general reserved prefix for built-in engine
  def isReservedPrefix(name: String): Boolean = name.startsWith("$") ||
    name.startsWith("pio_")

  // single entity reserved events
  val specialEvents = Set("$set", "$unset", "$delete")

  def isSpecialEvents(name: String): Boolean = specialEvents.contains(name)

  def validate(e: Event): Unit = {

    require(!e.event.isEmpty, "event must not be empty.")
    require(!e.entityType.isEmpty, "entityType must not be empty string.")
    require(!e.entityId.isEmpty, "entityId must not be empty string.")
    require(e.targetEntityType.map(!_.isEmpty).getOrElse(true),
      "targetEntityType must not be empty string")
    require(e.targetEntityId.map(!_.isEmpty).getOrElse(true),
      "targetEntityId must not be empty string.")
    require(!((e.targetEntityType != None) && (e.targetEntityId == None)),
      "targetEntityType and targetEntityId must be specified together.")
    require(!((e.targetEntityType == None) && (e.targetEntityId != None)),
      "targetEntityType and targetEntityId must be specified together.")
    require(!((e.event == "$unset") && e.properties.isEmpty),
      "properties cannot be empty for $unset event")
    require(!isReservedPrefix(e.event) || isSpecialEvents(e.event),
      s"${e.event} is not a supported reserved event name.")
    require(!isSpecialEvents(e.event) ||
      ((e.targetEntityType == None) && (e.targetEntityId == None)),
      s"Reserved event ${e.event} cannot have targetEntity")
    require(!isReservedPrefix(e.entityType) ||
      isBuiltinEntityTypes(e.entityType),
      s"The entityType ${e.entityType} is not allowed. " +
        s"'pio_' is a reserved name prefix.")
    require(e.targetEntityType.map{ t =>
      (!isReservedPrefix(t) || isBuiltinEntityTypes(t))}.getOrElse(true),
      s"The targetEntityType ${e.targetEntityType.get} is not allowed. " +
        s"'pio_' is a reserved name prefix.")
    validateProperties(e)
  }

  // properties
  val builtinEntityTypes: Set[String] = Set("pio_pr")
  val builtinProperties: Set[String] = Set()

  def isBuiltinEntityTypes(name: String): Boolean = builtinEntityTypes.contains(name)

  def validateProperties(e: Event): Unit = {
    e.properties.keySet.foreach { k =>
      require(!isReservedPrefix(k) || builtinProperties.contains(k),
        s"The property ${k} is not allowed. " +
          s"'pio_' is a reserved name prefix.")
    }
  }

}
