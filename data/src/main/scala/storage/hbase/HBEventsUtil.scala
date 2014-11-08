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
import io.prediction.data.storage.EventValidation
import io.prediction.data.storage.Events
import io.prediction.data.storage.DataMap

import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.filter.FilterList
import org.apache.hadoop.hbase.filter.RegexStringComparator
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp

import org.json4s.DefaultFormats
import org.json4s.JObject
import org.json4s.native.Serialization.{ read, write }

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import org.apache.commons.codec.binary.Base64
import java.security.MessageDigest

/* common utility function for acessing EventsStore in HBase */
object HBEventsUtil {

  implicit val formats = DefaultFormats

  def tableName(namespace: String, appId: Int) = s"${namespace}:events_${appId}"
  //val table = "events"

  // column nams for "e" column family
  val colNames: Map[String, Array[Byte]] = Map(
    "event" -> "e",
    "entityType" -> "ety",
    "entityId" -> "eid",
    "targetEntityType" -> "tety",
    "targetEntityId" -> "teid",
    "properties" -> "p",
    "predictionKey" -> "pk",
    "eventTimeZone" -> "etz",
    "creationTimeZone" -> "ctz"
  ).mapValues(Bytes.toBytes(_))

  /*
  class RowKey(
    val appId: Int,
    val millis: Long,
    val uuidLow: Long
  ) {
    lazy val toBytes: Array[Byte] = {
      // add UUID least significant bits for multiple actions at the same time
      // (UUID's most significantbits are actually timestamp,
      // use eventTime instead).
      Bytes.toBytes(appId) ++ Bytes.toBytes(millis) ++ Bytes.toBytes(uuidLow)
    }
    override def toString: String = {
      Base64.encodeBase64URLSafeString(toBytes)
    }
  }*/


  val md5 = MessageDigest.getInstance("MD5")

  def hash(entityType: String, entityId: String): Array[Byte] = {
    val s = entityType + "-" + entityId
    md5.digest(Bytes.toBytes(s))
  }

  class RowKey(
    val b: Array[Byte]
  ) {
    require((b.size == 32), s"Incorrect b size: ${b.size}")
    lazy val entityHash: Array[Byte] = b.slice(0, 16)
    lazy val millis: Long = Bytes.toLong(b.slice(16, 24))
    lazy val uuidLow: Long = Bytes.toLong(b.slice(24, 32))

    lazy val toBytes: Array[Byte] = b

    override def toString: String = {
      Base64.encodeBase64URLSafeString(toBytes)
    }
  }

  object RowKey {
    def apply(
      entityType: String,
      entityId: String,
      millis: Long,
      uuidLow: Long): RowKey = {
        // add UUID least significant bits for multiple actions at the same time
        // (UUID's most significantbits are actually timestamp,
        // use eventTime instead).
        val b = hash(entityType, entityId) ++
          Bytes.toBytes(millis) ++ Bytes.toBytes(uuidLow)
        new RowKey(b)
      }

    // get RowKey from string representation
    def apply(s: String): RowKey = {
      try {
        apply(Base64.decodeBase64(s))
      } catch {
        case e: Exception => throw new RowKeyException(
          s"Failed to convert String ${s} to RowKey because ${e}", e)
      }
    }

    def apply(b: Array[Byte]): RowKey = {
      if (b.size != 32) {
        val bString = b.mkString(",")
        throw new RowKeyException(
          s"Incorrect byte array size. Bytes: ${bString}.")
      }
      new RowKey(b)
    }

  }

  class RowKeyException(val msg: String, val cause: Exception)
    extends Exception(msg, cause) {
      def this(msg: String) = this(msg, null)
    }

  /*
  case class PartialRowKey(val appId: Int, val millis: Option[Long] = None) {
    val toBytes: Array[Byte] = {
      Bytes.toBytes(appId) ++
        (millis.map(Bytes.toBytes(_)).getOrElse(Array[Byte]()))
    }
  }*/

  case class PartialRowKey(entityType: String, entityId: String,
    millis: Option[Long] = None) {
    val toBytes: Array[Byte] = {
      hash(entityType, entityId) ++
        (millis.map(Bytes.toBytes(_)).getOrElse(Array[Byte]()))
    }
  }

  def resultToEvent(result: Result, appId: Int): Event = {
    val rowKey = RowKey(result.getRow())

    val eBytes = Bytes.toBytes("e")
    //val e = result.getFamilyMap(eBytes)

    def getStringCol(col: String): String = {
      val r = result.getValue(eBytes, colNames(col))
      require(r != null,
        s"Failed to get value for column ${col}. " +
        s"Rowkey: ${rowKey.toString} " +
        s"StringBinary: ${Bytes.toStringBinary(result.getRow())}.")

      Bytes.toString(r)
    }

    def getOptStringCol(col: String): Option[String] = {
      val r = result.getValue(eBytes, colNames(col))
      if (r == null)
        None
      else
        Some(Bytes.toString(r))
    }

    def getTimestamp(col: String): Long = {
      result.getColumnLatestCell(eBytes, colNames(col)).getTimestamp()
    }

    val event = getStringCol("event")
    val entityType = getStringCol("entityType")
    val entityId = getStringCol("entityId")
    val targetEntityType = getOptStringCol("targetEntityType")
    val targetEntityId = getOptStringCol("targetEntityId")
    val properties: DataMap = getOptStringCol("properties")
      .map(s => DataMap(read[JObject](s))).getOrElse(DataMap())
    val predictionKey = getOptStringCol("predictionKey")
    val eventTimeZone = getOptStringCol("eventTimeZone")
      .map(DateTimeZone.forID(_))
      .getOrElse(EventValidation.defaultTimeZone)
    val creationTimeZone = getOptStringCol("creationTimeZone")
      .map(DateTimeZone.forID(_))
      .getOrElse(EventValidation.defaultTimeZone)

    val creationTime: DateTime = new DateTime(
      getTimestamp("event"), creationTimeZone
    )

    Event(
      eventId = Some(RowKey(result.getRow()).toString),
      event = event,
      entityType = entityType,
      entityId = entityId,
      targetEntityType = targetEntityType,
      targetEntityId = targetEntityId,
      properties = properties,
      eventTime = new DateTime(rowKey.millis, eventTimeZone),
      tags = Seq(),
      appId = appId,
      predictionKey = predictionKey,
      creationTime = creationTime
    )
  }


  def createScan(
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String],
    reversed: Option[Boolean] = Some(false)): Scan = {

    val scan: Scan = (entityType, entityId) match {
      case (Some(et), Some(eid)) => {
        val start = PartialRowKey(et, eid,
          startTime.map(_.getMillis)).toBytes
        // if no untilTime, stop when reach next bytes of entityTypeAndId
        val stop = untilTime.map(t =>
            PartialRowKey(et, eid, Some(t.getMillis)).toBytes)
          .getOrElse(Bytes.incrementBytes(
            PartialRowKey(et, eid).toBytes, 1))

        if (reversed.getOrElse(false)) {
          // Reversed order.
          val s = new Scan(stop, start)
          s.setReversed(true)
        } else {
          new Scan(start, stop)
        }
      }
      case (_, _) => {
        val s = new Scan()
        // TODO: row filter for time
        s
      }
    }

    if ((entityType != None) || (entityId != None)) {
      val filters = new FilterList()
      val eBytes = Bytes.toBytes("e")
      entityType.foreach { etype =>
        val compType = new RegexStringComparator("^"+etype+"$")
        val filterType = new SingleColumnValueFilter(
          eBytes, colNames("entityType"), CompareOp.EQUAL, compType)
        filters.addFilter(filterType)
      }
      entityId.foreach { eid =>
        val compId = new RegexStringComparator("^"+eid+"$")
        val filterId = new SingleColumnValueFilter(
          eBytes, colNames("entityId"), CompareOp.EQUAL, compId)
        filters.addFilter(filterId)
      }
      scan.setFilter(filters)
    }

    scan
  }

}
