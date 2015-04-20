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

import org.joda.time.DateTime

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

/** Base trait of a data access object that returns [[Event]] related RDD data
  * structure.
  */
trait PEvents extends Serializable {

  @deprecated("Use PEventStore.find() instead.", "0.9.2")
  def getByAppIdAndTimeAndEntity(appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String])(sc: SparkContext): RDD[Event] = {
      find(
        appId = appId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = entityType,
        entityId = entityId,
        eventNames = None
      )(sc)
    }

  /** Read from database and return the events.
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
    * @param sc Spark context
    * @return RDD[Event]
    */
  // NOTE: Don't delete! make this private[prediction] instead when deprecate
  @deprecated("Use PEventStore.find() instead.", "0.9.2")
  def find(
    appId: Int,
    channelId: Option[Int] = None,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    entityType: Option[String] = None,
    entityId: Option[String] = None,
    eventNames: Option[Seq[String]] = None,
    targetEntityType: Option[Option[String]] = None,
    targetEntityId: Option[Option[String]] = None)(sc: SparkContext): RDD[Event]

  /** Aggregate properties of entities based on these special events:
    * \$set, \$unset, \$delete events.
    *
    * @param appId use events of this app ID
    * @param channelId use events of this channel ID (default channel if it's None)
    * @param entityType aggregate properties of the entities of this entityType
    * @param startTime use events with eventTime >= startTime
    * @param untilTime use events with eventTime < untilTime
    * @param required only keep entities with these required properties defined
    * @param sc Spark context
    * @return RDD[(String, PropertyMap)] RDD of entityId and PropetyMap pair
    */
  // NOTE: Don't delete! make this private[prediction] instead when deprecate
  @deprecated("Use PEventStore.aggregateProperties() instead.", "0.9.2")
  def aggregateProperties(
    appId: Int,
    channelId: Option[Int] = None,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None)
    (sc: SparkContext): RDD[(String, PropertyMap)]

  /** :: Experimental ::
    * Extract EntityMap[A] from events for the entityType
    * NOTE: it is local EntityMap[A]
    */
  @deprecated("Use PEventStore.aggregateProperties() instead.", "0.9.2")
  def extractEntityMap[A: ClassTag](
    appId: Int,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None)
    (sc: SparkContext)(extract: DataMap => A): EntityMap[A]

  /** :: Experimental ::
    * Write events to database
    *
    * @param events RDD of Event
    * @param appId the app ID
    * @param sc Spark Context
    */
  private[prediction] def write(events: RDD[Event], appId: Int)(sc: SparkContext): Unit =
    write(events, appId, None)(sc)

  /** :: Experimental ::
    * Write events to database
    *
    * @param events RDD of Event
    * @param appId the app ID
    * @param channelId  channel ID (default channel if it's None)
    * @param sc Spark Context
    */
  private[prediction] def write(
    events: RDD[Event], appId: Int, channelId: Option[Int])(sc: SparkContext): Unit

}
