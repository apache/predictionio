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

package io.prediction.data.store

import io.prediction.data.storage.Storage
import io.prediction.data.storage.StorageError
import io.prediction.data.storage.Event

import org.joda.time.DateTime

import scala.concurrent.Await
import scala.concurrent.TimeoutException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object LEventStore {

  val defaultTimeout = Duration(60, "seconds")

  @transient lazy private val eventsDb = Storage.getLEvents()

  def findSingleEntity(
    appName: String,
    entityType: String,
    entityId: String,
    channelName: Option[String] = None,
    eventNames: Option[Seq[String]] = None,
    targetEntityType: Option[Option[String]] = None,
    targetEntityId: Option[Option[String]] = None,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    limit: Option[Int] = None,
    latest: Boolean = true,
    timeout: Duration = defaultTimeout): Either[StorageError, Iterator[Event]] = {

    val (appId, channelId) = Common.appNameToId(appName, channelName)

    try {
      Await.result(eventsDb.futureFind(
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
        reversed = Some(latest)), timeout)

    } catch {
      case e: TimeoutException => Left(StorageError(s"${e}"))
    }
  }
}
