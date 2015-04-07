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
import io.prediction.data.storage.Event
import io.prediction.data.storage.PropertyMap

import org.joda.time.DateTime

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

case class EventStoreException(message: String, cause: Throwable)
  extends Exception(message, cause) {

  /** EventStore Exception with cause being set to null
    *
    * @param message the detail message
    */
  def this(message: String) = this(message, null)
}


object PEventStore {

  @transient lazy private val eventsDb = Storage.getPEvents()
  @transient lazy private val appsDb = Storage.getMetaDataApps()
  @transient lazy private val channelsDb = Storage.getMetaDataChannels()

  def find(
    appName: String,
    channelName: Option[String] = None,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    entityType: Option[String] = None,
    entityId: Option[String] = None,
    eventNames: Option[Seq[String]] = None,
    targetEntityType: Option[Option[String]] = None,
    targetEntityId: Option[Option[String]] = None
  )(sc: SparkContext): RDD[Event] = {

    val appOpt = appsDb.getByName(appName)

    appOpt.map { app =>
      val channelMap: Map[String, Int] = channelsDb.getByAppid(app.id)
        .map(c => (c.name, c.id)).toMap

      val channelId: Option[Int] = channelName.map { ch =>
        if (channelMap.contains(ch))
          channelMap(ch)
        else
          throw new EventStoreException(s"Invalid channel name ${ch}.")
      }

      eventsDb.find(
        appId = app.id,
        channelId = channelId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = entityType,
        entityId = entityId,
        eventNames = eventNames,
        targetEntityType = targetEntityType,
        targetEntityId = targetEntityId
      )(sc)
    }.getOrElse{
      throw new EventStoreException(s"Invalid app name ${appName}")
    }
  }


  def aggregateProperties(
    appName: String,
    entityType: String,
    channelName: Option[String] = None,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None)
    (sc: SparkContext): RDD[(String, PropertyMap)] = {
      val appOpt = appsDb.getByName(appName)

      appOpt.map { app =>
        val channelMap: Map[String, Int] = channelsDb.getByAppid(app.id)
          .map(c => (c.name, c.id)).toMap

        val channelId: Option[Int] = channelName.map { ch =>
          if (channelMap.contains(ch))
            channelMap(ch)
          else
            throw new EventStoreException(s"Invalid channel name ${ch}.")
        }

        eventsDb.aggregateProperties(
          appId = app.id,
          entityType = entityType,
          channelId = channelId,
          startTime = startTime,
          untilTime = untilTime,
          required = required
        )(sc)
      }.getOrElse{
        throw new EventStoreException(s"Invalid app name ${appName}")
      }
    }

}
