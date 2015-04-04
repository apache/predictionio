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

package io.prediction.data.storage.hbase

import io.prediction.data.storage.Event
import io.prediction.data.storage.DataMap
import io.prediction.data.storage.PropertyMap
import io.prediction.data.storage.PEvents
import io.prediction.data.storage.PEventAggregator
import io.prediction.data.storage.EntityMap
import io.prediction.data.storage.BiMap

import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat
import org.apache.hadoop.hbase.mapreduce.PIOHBaseUtil
import org.apache.hadoop.hbase.TableNotFoundException
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.mapreduce.OutputFormat
import org.apache.hadoop.io.Writable

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import grizzled.slf4j.Logging

import scala.reflect.ClassTag

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext._

class HBPEvents(client: HBClient, namespace: String)
  extends PEvents with Logging {

  def checkTableExists(appId: Int, channelId: Option[Int]): Unit = {
    if (!client.admin.tableExists(HBEventsUtil.tableName(namespace, appId, channelId))) {
      if (channelId.isDefined) {
        error(s"The appId ${appId} with channelId ${channelId} does not exist." +
          s" Please use valid appId and channelId.")
        throw new Exception(s"HBase table not found for appId ${appId}" +
          s" with channelId ${channelId}.")
      } else {
        error(s"The appId ${appId} does not exist. Please use valid appId.")
        throw new Exception(s"HBase table not found for appId ${appId}.")
      }
    }
  }

  override
  def find(
    appId: Int,
    channelId: Option[Int] = None,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    entityType: Option[String] = None,
    entityId: Option[String] = None,
    eventNames: Option[Seq[String]] = None,
    targetEntityType: Option[Option[String]] = None,
    targetEntityId: Option[Option[String]] = None
    )(sc: SparkContext): RDD[Event] = {

    checkTableExists(appId, channelId)

    val conf = HBaseConfiguration.create()
    conf.set(TableInputFormat.INPUT_TABLE,
      HBEventsUtil.tableName(namespace, appId, channelId))

    val scan = HBEventsUtil.createScan(
        startTime = startTime,
        untilTime = untilTime,
        entityType = entityType,
        entityId = entityId,
        eventNames = eventNames,
        targetEntityType = targetEntityType,
        targetEntityId = targetEntityId,
        reversed = None)
    scan.setCaching(500) // TODO
    scan.setCacheBlocks(false) // TODO

    conf.set(TableInputFormat.SCAN, PIOHBaseUtil.convertScanToString(scan))

    // HBase is not accessed until this rdd is actually used.
    val rdd = sc.newAPIHadoopRDD(conf, classOf[TableInputFormat],
      classOf[ImmutableBytesWritable],
      classOf[Result]).map {
        case (key, row) => HBEventsUtil.resultToEvent(row, appId)
      }

    rdd
  }

  override
  def aggregateProperties(
    appId: Int,
    channelId: Option[Int] = None,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None)
    (sc: SparkContext): RDD[(String, PropertyMap)] = {

    checkTableExists(appId, channelId)

    val eventRDD = find(
      appId = appId,
      channelId = channelId,
      startTime = startTime,
      untilTime = untilTime,
      entityType = Some(entityType),
      eventNames = Some(PEventAggregator.eventNames))(sc)

    val dmRDD = try {
      PEventAggregator.aggregateProperties(eventRDD)
    } catch {
      case e: TableNotFoundException => {
        if (channelId.isDefined) {
          error(s"The appId ${appId} with channelId ${channelId} does not exist." +
            s" Please use valid appId and channelId.")
        } else {
          error(s"The appId ${appId} does not exist. Please use valid appId.")
        }
        throw e
      }
      case e: Exception => throw e
    }

    if (required.isDefined) {
      dmRDD.filter { case (k, v) =>
        required.get.map(v.contains(_)).reduce(_ && _)
      }
    } else dmRDD

  }

  override
  def extractEntityMap[A: ClassTag](
    appId: Int,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None)
    (sc: SparkContext)(extract: DataMap => A): EntityMap[A] = {

    val idToData: Map[String, A] = aggregateProperties(
      appId = appId,
      entityType = entityType,
      startTime = startTime,
      untilTime = untilTime,
      required = required
    )(sc).map{ case (id, dm) =>
      try {
        (id, extract(dm))
      } catch {
        case e: Exception => {
          logger.error(s"Failed to get extract entity from DataMap ${dm} of" +
            s" entityId ${id}. Exception: ${e}.")
          throw e
        }
      }
    }.collectAsMap.toMap

    new EntityMap(idToData)
  }

  override
  def write(
    events: RDD[Event], appId: Int, channelId: Option[Int])(sc: SparkContext): Unit = {

    checkTableExists(appId, channelId)

    val conf = HBaseConfiguration.create()
    conf.set(TableOutputFormat.OUTPUT_TABLE,
      HBEventsUtil.tableName(namespace, appId, channelId))
    conf.setClass("mapreduce.outputformat.class",
      classOf[TableOutputFormat[Object]],
      classOf[OutputFormat[Object, Writable]])

    events.map { event =>
      val (put, rowKey) = HBEventsUtil.eventToPut(event, appId)
      (new ImmutableBytesWritable(rowKey.toBytes), put)
    }.saveAsNewAPIHadoopDataset(conf)

  }

}
