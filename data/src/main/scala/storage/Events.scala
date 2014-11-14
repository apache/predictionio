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

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.json4s.Formats

import scala.concurrent.ExecutionContext

import org.apache.commons.codec.binary.Base64

// separate Event for API before require mo
case class Event(
  val eventId: Option[String] = None,
  val event: String,
  val entityType: String,
  val entityId: String,
  val targetEntityType: Option[String] = None,
  val targetEntityId: Option[String] = None,
  val properties: DataMap = DataMap(), // default empty
  val eventTime: DateTime = DateTime.now,
  val tags: Seq[String] = Seq(),
  val prId: Option[String] = None,
  val creationTime: DateTime = DateTime.now
) {
  override def toString() = {
    s"Event(id=$eventId,event=$event,eType=$entityType,eId=$entityId," +
    s"tType=$targetEntityType,tId=$targetEntityId,p=$properties,t=$eventTime," +
    s"tags=$tags,pKey=$prId,ct=$creationTime)"
  }
}

object EventValidation {

  val defaultTimeZone = DateTimeZone.UTC

  // general reserved prefix for built-in engine
  def isReservedPrefix(name: String): Boolean = name.startsWith("$") ||
    name.startsWith("pio_")

  // single entity reserved events
  val specialEvents = Set("$set", "$unset", "$delete")

  def isSpecialEvents(name: String): Boolean = specialEvents.contains(name)

  def validate(e: Event) = {

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
      s"Reserved entityType ${e.entityType} is not supported.")
    require(e.targetEntityType.map{ t =>
      (!isReservedPrefix(t) || isBuiltinEntityTypes(t))}.getOrElse(true),
      s"Reserved targetEntityType ${e.targetEntityType} is not supported.")
    validateProperties(e)
  }

  // properties
  val builtinEntityTypes = Set("pio_user", "pio_item", "pio_pr")
  val builtinProperties = Set(
    "pio_itypes", "pio_starttime", "pio_endtime",
    "pio_inactive", "pio_price", "pio_rating")

  def isBuiltinEntityTypes(name: String) = builtinEntityTypes.contains(name)

  def validateProperties(e: Event) = {
    e.properties.keySet.foreach { k =>
      require(!isReservedPrefix(k) || builtinProperties.contains(k),
        s"Reserved proeprties ${k} is not supported.")
    }
  }

}

trait Events {

  private def notImplemented(implicit ec: ExecutionContext) = Future {
    Left(StorageError("Not implemented."))
  }
  val timeout = Duration(5, "seconds")

  /** Initialize Event Store for the appId.
   * initailization routine to be called when app is first created.
   * return true if succeed or false if fail.
   */
  def init(appId: Int): Boolean = {
    throw new Exception("init() is not implemented.")
    false
  }

  /** Remove Event Store for this appId */
  def remove(appId: Int): Boolean = {
    throw new Exception("remove() is not implemented.")
    false
  }

  /** Close this Event Store interface object.
   * (Eg. close connection, release resources)
   */
  def close(): Unit = {
    throw new Exception("close() is not implemented.")
    ()
  }

  def futureInsert(event: Event, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, String]] =
    notImplemented

  def futureGet(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Option[Event]]] =
    notImplemented

  def futureDelete(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Boolean]] =
    notImplemented

  def futureGetByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = notImplemented

  /* where t >= start and t < untilTime */
  def futureGetByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = notImplemented

  def futureGetByAppIdAndTimeAndEntity(appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]]  = notImplemented

  // limit: None or -1: Get all events
  // order: Either 1 or -1. 1: From earliest; -1: From latest;
  def futureGetGeneral(
    appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String],
    limit: Option[Int] = None,
    reversed: Option[Boolean] = Some(false))(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]]  = notImplemented

  def futureDeleteByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Unit]] = notImplemented

  // following is blocking
  def insert(event: Event, appId: Int)(implicit ec: ExecutionContext):
    Either[StorageError, String] = {
    Await.result(futureInsert(event, appId), timeout)
  }

  def get(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Either[StorageError, Option[Event]] = {
    Await.result(futureGet(eventId, appId), timeout)
  }

  def delete(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Either[StorageError, Boolean] = {
    Await.result(futureDelete(eventId, appId), timeout)
  }

  def getByAppId(appId: Int)(implicit ec: ExecutionContext):
    Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppId(appId), timeout)
  }

  def getByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime])(implicit ec: ExecutionContext):
    Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppIdAndTime(appId, startTime, untilTime), timeout)
  }

  def getByAppIdAndTimeAndEntity(appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String])(implicit ec: ExecutionContext):
    Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppIdAndTimeAndEntity(appId, startTime, untilTime,
      entityType, entityId), timeout)
  }

  def deleteByAppId(appId: Int)(implicit ec: ExecutionContext):
    Either[StorageError, Unit] = {
    Await.result(futureDeleteByAppId(appId), timeout)
  }

}
