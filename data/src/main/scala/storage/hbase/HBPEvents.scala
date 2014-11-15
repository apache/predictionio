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

package io.prediction.data.storage.hbase

import io.prediction.data.storage.Event
import io.prediction.data.storage.PEvents

import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.mapreduce.PIOHBaseUtil

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import grizzled.slf4j.Logging

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class HBPEvents(client: HBClient, namespace: String)
  extends PEvents with Logging {

  override
  def getGeneral(
    appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String],
    eventNames: Option[Seq[String]])(sc: SparkContext): RDD[Event] = {

    val conf = HBaseConfiguration.create()
    conf.set(TableInputFormat.INPUT_TABLE,
      HBEventsUtil.tableName(namespace, appId))

    val scan = HBEventsUtil.createScan(
        startTime = startTime,
        untilTime = untilTime,
        entityType = entityType,
        entityId = entityId,
        eventNames = eventNames,
        reversed = None)
    scan.setCaching(500) // TODO
    scan.setCacheBlocks(false) // TODO

    conf.set(TableInputFormat.SCAN, PIOHBaseUtil.convertScanToString(scan))

    val rdd = sc.newAPIHadoopRDD(conf, classOf[TableInputFormat],
      classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable],
      classOf[Result]).map {
        case (key, row) => HBEventsUtil.resultToEvent(row, appId)
      }

    rdd
  }

}
