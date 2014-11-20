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
