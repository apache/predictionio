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

import io.prediction.annotation.Experimental

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.concurrent.TimeoutException

import org.joda.time.DateTime

/** Base trait of a data access object that directly returns [[Event]] without
  * going through Spark's parallelization.
  */
trait LEvents {

  private def notImplemented(implicit ec: ExecutionContext) = Future {
    throw new StorageException("Not implemented.")
  }

  val defaultTimeout = Duration(60, "seconds")

  /** Initialize Event Store for the appId.
    * initialization routine to be called when app is first created.
    * return true if succeed or false if fail.
    * @param appId App ID
    * @param channelId Channel ID
    * @return status. true if succeeded; false if failed.
    */
  private[prediction] def init(appId: Int, channelId: Option[Int] = None): Boolean = {
    throw new Exception("init() is not implemented.")
    false
  }

  /** Remove Event Store for this appId
    * @param appId App ID
    * @param channelId Channel ID
    * @return status. true if succeeded; false if failed.
    */
  private[prediction] def remove(appId: Int, channelId: Option[Int] = None): Boolean = {
    throw new Exception("remove() is not implemented.")
    false
  }

  /** Close this Event Store interface object.
   * (Eg. close connection, release resources)
   */
  private[prediction] def close(): Unit = {
    throw new Exception("close() is not implemented.")
    ()
  }

  /* auxiliary */
  private[prediction]
  def futureInsert(event: Event, appId: Int)(implicit ec: ExecutionContext):
    Future[String] = futureInsert(event, appId, None)

  private[prediction]
  def futureInsert(
    event: Event, appId: Int, channelId: Option[Int])(implicit ec: ExecutionContext):
    Future[String] =
    notImplemented

  /* auxiliary */
  private[prediction]
  def futureGet(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Future[Option[Event]] = futureGet(eventId, appId, None)

  private[prediction]
  def futureGet(
    eventId: String, appId: Int, channelId: Option[Int])(implicit ec: ExecutionContext):
    Future[Option[Event]] =
    notImplemented

  /* auxiliary */
  private[prediction]
  def futureDelete(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Future[Boolean] = futureDelete(eventId, appId, None)

  private[prediction]
  def futureDelete(
    eventId: String, appId: Int, channelId: Option[Int])(implicit ec: ExecutionContext):
    Future[Boolean] =
    notImplemented

  /** Reads from database and returns a Future of events iterator.
    *
    * @param appId return events of this app ID
    * @param channelId return events of this channel ID (default channel if it's None)
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
    * @return Future[Iterator[Event]]
    */
  private[prediction] def futureFind(
    appId: Int,
    channelId: Option[Int] = None,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    entityType: Option[String] = None,
    entityId: Option[String] = None,
    eventNames: Option[Seq[String]] = None,
    targetEntityType: Option[Option[String]] = None,
    targetEntityId: Option[Option[String]] = None,
    limit: Option[Int] = None,
    reversed: Option[Boolean] = None)(implicit ec: ExecutionContext):
    Future[Iterator[Event]] = notImplemented

  /** Aggregate properties of entities based on these special events:
    * \$set, \$unset, \$delete events.
    * and returns a Future of Map of entityId to properties.
    *
    * @param appId use events of this app ID
    * @param channelId use events of this channel ID (default channel if it's None)
    * @param entityType aggregate properties of the entities of this entityType
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param required only keep entities with these required properties defined
    * @param ec ExecutionContext
    * @return Future[Map[String, PropertyMap]]
    */
  private[prediction] def futureAggregateProperties(
    appId: Int,
    channelId: Option[Int] = None,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None)(implicit ec: ExecutionContext):
    Future[Map[String, PropertyMap]] = {
      futureFind(
        appId = appId,
        channelId = channelId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = Some(entityType),
        eventNames = Some(LEventAggregator.eventNames)
      ).map{ eventIt =>
        val dm = LEventAggregator.aggregateProperties(eventIt)
        if (required.isDefined) {
          dm.filter { case (k, v) =>
            required.get.map(v.contains(_)).reduce(_ && _)
          }
        } else dm
      }
    }

  /**
    * :: Experimental ::
    *
    * Aggregate properties of the specified entity (entityType + entityId)
    * based on these special events:
    * \$set, \$unset, \$delete events.
    * and returns a Future of Option[PropertyMap]
    *
    * @param appId use events of this app ID
    * @param channelId use events of this channel ID (default channel if it's None)
    * @param entityType the entityType
    * @param entityId the entityId
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param ec ExecutionContext
    * @return Future[Option[PropertyMap]]
    */
  @Experimental
  private[prediction] def futureAggregatePropertiesOfEntity(
    appId: Int,
    channelId: Option[Int] = None,
    entityType: String,
    entityId: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None)(implicit ec: ExecutionContext):
    Future[Option[PropertyMap]] = {
      futureFind(
        appId = appId,
        channelId = channelId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = Some(entityType),
        entityId = Some(entityId),
        eventNames = Some(LEventAggregator.eventNames)
      ).map{ eventIt =>
        LEventAggregator.aggregatePropertiesSingle(eventIt)
      }
    }

  // following is blocking
  private[prediction] def insert(event: Event, appId: Int,
    channelId: Option[Int] = None,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    String = {
    Await.result(futureInsert(event, appId, channelId), timeout)
  }

  private[prediction] def get(eventId: String, appId: Int,
    channelId: Option[Int] = None,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Option[Event] = {
    Await.result(futureGet(eventId, appId, channelId), timeout)
  }

  private[prediction] def delete(eventId: String, appId: Int,
    channelId: Option[Int] = None,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Boolean = {
    Await.result(futureDelete(eventId, appId, channelId), timeout)
  }

  /** reads from database and returns events iterator.
    *
    * @param appId return events of this app ID
    * @param channelId return events of this channel ID (default channel if it's None)
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
    * @param reversed Reverse the order (should be used with both
    *   targetEntityType and targetEntityId specified)
    *   - return oldest events first if None or Some(false) (default)
    *   - return latest events first if Some(true)
    * @param ec ExecutionContext
    * @return Iterator[Event]
    */
  private[prediction] def find(
    appId: Int,
    channelId: Option[Int] = None,
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
    Iterator[Event] = {
      Await.result(futureFind(
        appId = appId,
        channelId = channelId,
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

  // NOTE: remove in next release
  @deprecated("Use find() instead.", "0.9.2")
  private[prediction] def findLegacy(
    appId: Int,
    channelId: Option[Int] = None,
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
      try {
        // return Either for legacy usage
        Right(Await.result(futureFind(
          appId = appId,
          channelId = channelId,
          startTime = startTime,
          untilTime = untilTime,
          entityType = entityType,
          entityId = entityId,
          eventNames = eventNames,
          targetEntityType = targetEntityType,
          targetEntityId = targetEntityId,
          limit = limit,
          reversed = reversed), timeout))
      } catch {
        case e: TimeoutException => Left(StorageError(s"${e}"))
        case e: Exception => Left(StorageError(s"${e}"))
      }
  }

  /** reads events of the specified entity.
    *
    * @param appId return events of this app ID
    * @param channelId return events of this channel ID (default channel if it's None)
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
    * @param startTime return events with eventTime >= startTime
    * @param untilTime return events with eventTime < untilTime
    * @param limit Limit number of events. Get all events if None or Some(-1)
    * @param latest Return latest event first (default true)
    * @param ec ExecutionContext
    * @return Either[StorageError, Iterator[Event]]
    */
  // NOTE: remove this function in next release
  @deprecated("Use LEventStore.findByEntity() instead.", "0.9.2")
  def findSingleEntity(
    appId: Int,
    channelId: Option[Int] = None,
    entityType: String,
    entityId: String,
    eventNames: Option[Seq[String]] = None,
    targetEntityType: Option[Option[String]] = None,
    targetEntityId: Option[Option[String]] = None,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    limit: Option[Int] = None,
    latest: Boolean = true,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Either[StorageError, Iterator[Event]] = {

    findLegacy(
      appId = appId,
      channelId = channelId,
      startTime = startTime,
      untilTime = untilTime,
      entityType = Some(entityType),
      entityId = Some(entityId),
      eventNames = eventNames,
      targetEntityType = targetEntityType,
      targetEntityId = targetEntityId,
      limit = limit,
      reversed = Some(latest),
      timeout = timeout)

  }

  /** Aggregate properties of entities based on these special events:
    * \$set, \$unset, \$delete events.
    * and returns a Map of entityId to properties.
    *
    * @param appId use events of this app ID
    * @param channelId use events of this channel ID (default channel if it's None)
    * @param entityType aggregate properties of the entities of this entityType
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param required only keep entities with these required properties defined
    * @param ec ExecutionContext
    * @return Map[String, PropertyMap]
    */
  private[prediction] def aggregateProperties(
    appId: Int,
    channelId: Option[Int] = None,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Map[String, PropertyMap] = {
    Await.result(futureAggregateProperties(
      appId = appId,
      channelId = channelId,
      entityType = entityType,
      startTime = startTime,
      untilTime = untilTime,
      required = required), timeout)
  }

  /**
    * :: Experimental ::
    *
    * Aggregate properties of the specified entity (entityType + entityId)
    * based on these special events:
    * \$set, \$unset, \$delete events.
    * and returns Option[PropertyMap]
    *
    * @param appId use events of this app ID
    * @param channelId use events of this channel ID
    * @param entityType the entityType
    * @param entityId the entityId
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param ec ExecutionContext
    * @return Future[Option[PropertyMap]]
    */
  @Experimental
  private[prediction] def aggregatePropertiesOfEntity(
    appId: Int,
    channelId: Option[Int] = None,
    entityType: String,
    entityId: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    timeout: Duration = defaultTimeout)(implicit ec: ExecutionContext):
    Option[PropertyMap] = {

    Await.result(futureAggregatePropertiesOfEntity(
      appId = appId,
      channelId = channelId,
      entityType = entityType,
      entityId = entityId,
      startTime = startTime,
      untilTime = untilTime), timeout)
  }

}
