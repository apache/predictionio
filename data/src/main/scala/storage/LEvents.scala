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

import scala.concurrent.ExecutionContext

private[prediction] trait LEvents {

  private def notImplemented(implicit ec: ExecutionContext) = Future {
    Left(StorageError("Not implemented."))
  }
  val defaultTimeout = Duration(60, "seconds")

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
    Future[Either[StorageError, Iterator[Event]]] = notImplemented

  /** reads from database and returns a Future of either StorageError or
    * events iterator.
    *
    * @param appId return events of this app ID
    * @param startTime return events with eventTime >= startTime
    * @param untilTime return events with eventTime < untilTime
    * @param entityType return events of this entityType
    * @param entityId return events of this entityId
    * @param eventNames return events with any of these event names.
    * @param targetEntityType return events of this targetEntityType:
    *   - None means no restriction on targetEntityType
    *   - Some(None) means no targetEntityType for this event
    *   - Some(Some(x)) means targetEntityType should match x.
    * @param targetEntityId return events of this targetEntityId
    *   - None means no restriction on targetEntityId
    *   - Some(None) means no targetEntityId for this event
    *   - Some(Some(x)) means targetEntityId should match x.
    * @param limit Limit number of events. Get all events if None or Some(-1)
    * @param reversed Reverse the order.
    *   - return oldest events first if None or Some(false) (default)
    *   - return latest events first if Some(true)
    * @param ec ExecutionContext
    * @return Future[Either[StorageError, Iterator[Event]]]
    */
  def futureFind(
    appId: Int,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    entityType: Option[String] = None,
    entityId: Option[String] = None,
    eventNames: Option[Seq[String]] = None,
    targetEntityType: Option[Option[String]] = None,
    targetEntityId: Option[Option[String]] = None,
    limit: Option[Int] = None,
    reversed: Option[Boolean] = None)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = notImplemented

  /** Aggregate properties of entities based on these special events:
    * \$set, \$unset, \$delete events.
    * and returns a Future of either StorageError or a Map of entityId to
    * properties.
    *
    * @param appId use events of this app ID
    * @param entityType aggregate properties of the entities of this entityType
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param required only keep entities with these required properties defined
    * @param ec ExecutionContext
    * @return Future[Either[StorageError, Map[String, DataMap]]]
    */
  def futureAggregateProperties(
    appId: Int,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Map[String, DataMap]]] = notImplemented

  /** Experimental.
    *
    * Aggregate properties of the specified entity (entityType + entityId)
    * based on these special events:
    * \$set, \$unset, \$delete events.
    * and returns a Future of either StorageError or Option[DataMap]
    *
    * @param appId use events of this app ID
    * @param entityType the entityType
    * @param entityId the entityId
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param ec ExecutionContext
    * @return Future[Either[StorageError, Option[DataMap]]]
    */
  def futureAggregatePropertiesSingle(
    appId: Int,
    entityType: String,
    entityId: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Option[DataMap]]] = notImplemented

  def futureDeleteByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Unit]] = notImplemented

  // following is blocking
  def insert(event: Event, appId: Int,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, String] = {
    Await.result(futureInsert(event, appId), timeout)
  }

  def get(eventId: String, appId: Int,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Option[Event]] = {
    Await.result(futureGet(eventId, appId), timeout)
  }

  def delete(eventId: String, appId: Int,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Boolean] = {
    Await.result(futureDelete(eventId, appId), timeout)
  }

  def getByAppId(appId: Int,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppId(appId), timeout)
  }

  def getByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime],
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppIdAndTime(appId, startTime, untilTime), timeout)
  }

  def getByAppIdAndTimeAndEntity(appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String],
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppIdAndTimeAndEntity(appId, startTime, untilTime,
      entityType, entityId), timeout)
  }

  /** reads from database and returns either StorageError or
    * events iterator.
    *
    * @param appId return events of this app ID
    * @param startTime return events with eventTime >= startTime
    * @param untilTime return events with eventTime < untilTime
    * @param entityType return events of this entityType
    * @param entityId return events of this entityId
    * @param eventNames return events with any of these event names.
    * @param targetEntityType return events of this targetEntityType:
    *   - None means no restriction on targetEntityType
    *   - Some(None) means no targetEntityType for this event
    *   - Some(Some(x)) means targetEntityType should match x.
    * @param targetEntityId return events of this targetEntityId
    *   - None means no restriction on targetEntityId
    *   - Some(None) means no targetEntityId for this event
    *   - Some(Some(x)) means targetEntityId should match x.
    * @param limit Limit number of events. Get all events if None or Some(-1)
    * @param reversed Reverse the order.
    *   - return oldest events first if None or Some(false) (default)
    *   - return latest events first if Some(true)
    * @param ec ExecutionContext
    * @return Either[StorageError, Iterator[Event]]
    */
  def find(
    appId: Int,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    entityType: Option[String] = None,
    entityId: Option[String] = None,
    eventNames: Option[Seq[String]] = None,
    targetEntityType: Option[Option[String]] = None,
    targetEntityId: Option[Option[String]] = None,
    limit: Option[Int] = None,
    reversed: Option[Boolean] = None,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Iterator[Event]] = {
    Await.result(futureFind(
      appId = appId,
      startTime = startTime,
      untilTime = untilTime,
      entityType = entityType,
      entityId = entityId,
      eventNames = eventNames,
      targetEntityType = targetEntityType,
      targetEntityId = targetEntityId,
      limit = limit,
      reversed = reversed), timeout)
  }

  /** Aggregate properties of entities based on these special events:
    * \$set, \$unset, \$delete events.
    * and returns either StorageError or a Map of entityId to
    * properties.
    *
    * @param appId use events of this app ID
    * @param entityType aggregate properties of the entities of this entityType
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param required only keep entities with these required properties defined
    * @param ec ExecutionContext
    * @return Either[StorageError, Map[String, DataMap]]
    */
  def aggregateProperties(
    appId: Int,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Map[String, DataMap]] = {
    Await.result(futureAggregateProperties(
      appId = appId,
      entityType = entityType,
      startTime = startTime,
      untilTime = untilTime,
      required = required), timeout)
  }

  /** Experimental.
    *
    * Aggregate properties of the specified entity (entityType + entityId)
    * based on these special events:
    * \$set, \$unset, \$delete events.
    * and returns either StorageError or Option[DataMap]
    *
    * @param appId use events of this app ID
    * @param entityType the entityType
    * @param entityId the entityId
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param ec ExecutionContext
    * @return Future[Either[StorageError, Option[DataMap]]]
    */
  def aggregatePropertiesSingle(
    appId: Int,
    entityType: String,
    entityId: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Option[DataMap]] = {

    Await.result(futureAggregatePropertiesSingle(
      appId = appId,
      entityType = entityType,
      entityId = entityId,
      startTime = startTime,
      untilTime = untilTime), timeout)
  }

  def deleteByAppId(appId: Int,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Unit] = {
    Await.result(futureDeleteByAppId(appId), timeout)
  }

}
