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
import io.prediction.data.storage.EventID
import io.prediction.data.storage.EventValidation
import io.prediction.data.storage.Events
import io.prediction.data.storage.EventJson4sSupport
import io.prediction.data.storage.DataMap
import io.prediction.data.storage.StorageError

import grizzled.slf4j.Logging

import org.json4s.DefaultFormats
import org.json4s.JObject
import org.json4s.native.Serialization.{ read, write }
//import org.json4s.ext.JodaTimeSerializers

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import org.apache.hadoop.hbase.NamespaceDescriptor
import org.apache.hadoop.hbase.NamespaceExistException
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.HTable
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Delete
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.filter.FilterList
import org.apache.hadoop.hbase.filter.RegexStringComparator
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp

import scala.collection.JavaConversions._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import java.util.UUID

import org.apache.commons.codec.binary.Base64

class HBEvents(client: HBClient, namespace: String) extends Events with Logging {

  // check namespace exist
  val existingNamespace = client.admin.listNamespaceDescriptors().map(_.getName)
  if (!existingNamespace.contains(namespace)) {
    val nameDesc = NamespaceDescriptor.create(namespace).build()
    info(s"The namespace ${namespace} doesn't exist yet. Creating now...")
    client.admin.createNamespace(nameDesc)
  }
  /*
  try {
    val nameDesc = NamespaceDescriptor.create(namespace).build()
    client.admin.createNamespace(nameDesc)
  } catch {
    case e: NamespaceExistException => info(s"namespace already existed: ${e}")
    case e: Exception => throw new RuntimeException(e)
  }
  */

  implicit val formats = DefaultFormats + new EventJson4sSupport.DBSerializer
  //implicit val formats = DefaultFormats.lossless ++ JodaTimeSerializers.all

  val tableName = TableName.valueOf(namespace, "events")
  //val table = new HTable(client.conf, tableName)

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

  // create table if not exist
  if (!client.admin.tableExists(tableName)) {
    val tableDesc = new HTableDescriptor(tableName)
    tableDesc.addFamily(new HColumnDescriptor("e"))
    tableDesc.addFamily(new HColumnDescriptor("r")) // reserved
    client.admin.createTable(tableDesc)
  }

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
  }

  case class PartialRowKey(val appId: Int, val millis: Option[Long] = None) {
    val toBytes: Array[Byte] = {
      Bytes.toBytes(appId) ++
        (millis.map(Bytes.toBytes(_)).getOrElse(Array[Byte]()))
    }
  }

  object RowKey {
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
      if (b.size != 20) {
        val bString = b.mkString(",")
        throw new RowKeyException(
          s"Incorrect byte array size. Bytes: ${bString}.")
      }

      new RowKey(
        appId = Bytes.toInt(b.slice(0, 4)),
        millis = Bytes.toLong(b.slice(4, 12)),
        uuidLow = Bytes.toLong(b.slice(12, 20))
      )
    }
  }

  class RowKeyException(msg: String, cause: Exception)
    extends Exception(msg, cause) {
      def this(msg: String) = this(msg, null)
    }

  override
  def futureInsert(event: Event)(implicit ec: ExecutionContext):
    Future[Either[StorageError, String]] = {
    Future {
      //val table = new HTable(client.conf, tableName)
      val table = client.connection.getTable(tableName)
      val uuidLow: Long = UUID.randomUUID().getLeastSignificantBits
      val rowKey = new RowKey(
        appId = event.appId,
        millis = event.eventTime.getMillis,
        uuidLow = uuidLow
      )

      val eBytes = Bytes.toBytes("e")
      // use creationTime as HBase's cell timestamp
      val put = new Put(rowKey.toBytes, event.creationTime.getMillis)

      def addStringToE(col: Array[Byte], v: String) = {
        put.add(eBytes, col, Bytes.toBytes(v))
      }

      addStringToE(colNames("event"), event.event)
      addStringToE(colNames("entityType"), event.entityType)
      addStringToE(colNames("entityId"), event.entityId)

      event.targetEntityType.foreach { targetEntityType =>
        addStringToE(colNames("targetEntityType"), targetEntityType)
      }

      event.targetEntityId.foreach { targetEntityId =>
        addStringToE(colNames("targetEntityId"), targetEntityId)
      }

      // TODO: make properties Option[]
      if (!event.properties.isEmpty) {
        addStringToE(colNames("properties"), write(event.properties.toJObject))
      }

      event.predictionKey.foreach { predictionKey =>
        addStringToE(colNames("predictionKey"), predictionKey)
      }

      val eventTimeZone = event.eventTime.getZone
      if (!eventTimeZone.equals(EventValidation.defaultTimeZone)) {
        addStringToE(colNames("eventTimeZone"), eventTimeZone.getID)
      }

      val creationTimeZone = event.creationTime.getZone
      if (!creationTimeZone.equals(EventValidation.defaultTimeZone)) {
        addStringToE(colNames("creationTimeZone"), creationTimeZone.getID)
      }

      // can use zero-length byte array for tag cell value

      table.put(put)
      table.flushCommits()
      table.close()
      Right(rowKey.toString)
    }/*.recover {
      case e: Exception => Left(StorageError(e.toString))
    }*/
  }

  private def resultToEvent(result: Result): Event = {
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
      eventId = Some(EventID(result.getRow())),
      event = event,
      entityType = entityType,
      entityId = entityId,
      targetEntityType = targetEntityType,
      targetEntityId = targetEntityId,
      properties = properties,
      eventTime = new DateTime(rowKey.millis, eventTimeZone),
      tags = Seq(),
      appId = rowKey.appId,
      predictionKey = predictionKey,
      creationTime = creationTime
    )
  }

  override
  def futureGet(eventId: String)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Option[Event]]] = {
      Future {
        val table = client.connection.getTable(tableName)
        val rowKey = RowKey(eventId)
        val get = new Get(rowKey.toBytes)

        val result = table.get(get)
        table.close()

        if (!result.isEmpty()) {
          val event = resultToEvent(result)
          Right(Some(event))
        } else {
          Right(None)
        }
      }.recover {
        case e: RowKeyException => Left(StorageError(e.toString))
        case e: Exception => throw e
      }
    }

  override
  def futureDelete(eventId: String)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Boolean]] = {
    Future {
      val table = client.connection.getTable(tableName)
      val rowKey = RowKey(eventId)
      val exists = table.exists(new Get(rowKey.toBytes))
      table.delete(new Delete(rowKey.toBytes))
      table.close()
      Right(exists)
    }
  }

  override
  def futureGetByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      Future {
        val table = client.connection.getTable(tableName)
        val start = PartialRowKey(appId)
        val stop = PartialRowKey(appId+1)
        val scan = new Scan(start.toBytes, stop.toBytes)
        val scanner = table.getScanner(scan)
        table.close()
        Right(scanner.iterator().map { resultToEvent(_) })
      }
    }

  override
  def futureGetByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      Future {
        val table = client.connection.getTable(tableName)
        val start = PartialRowKey(appId, startTime.map(_.getMillis))
        // if no untilTime, stop when reach next appId
        val stop = untilTime.map(t => PartialRowKey(appId, Some(t.getMillis)))
          .getOrElse(PartialRowKey(appId+1))
        val scan = new Scan(start.toBytes, stop.toBytes)
        val scanner = table.getScanner(scan)
        table.close()
        Right(scanner.iterator().map { resultToEvent(_) })
      }
  }

  override
  def futureGetByAppIdAndTimeAndEntity(appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      Future {
        val table = client.connection.getTable(tableName)
        val start = PartialRowKey(appId, startTime.map(_.getMillis))
        // if no untilTime, stop when reach next appId
        val stop = untilTime.map(t => PartialRowKey(appId, Some(t.getMillis)))
          .getOrElse(PartialRowKey(appId+1))
        val scan = new Scan(start.toBytes, stop.toBytes)

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
        val scanner = table.getScanner(scan)
        table.close()
        Right(scanner.iterator().map { resultToEvent(_) })
      }
  }

  override
  def futureDeleteByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Unit]] = {
    Future {
      // TODO: better way to handle range delete
      val table = client.connection.getTable(tableName)
      val start = PartialRowKey(appId)
      val stop = PartialRowKey(appId+1)
      val scan = new Scan(start.toBytes, stop.toBytes)
      val scanner = table.getScanner(scan)
      val it = scanner.iterator()
      while (it.hasNext()) {
        val result = it.next()
        table.delete(new Delete(result.getRow()))
      }
      scanner.close()
      table.close()
      Right(())
    }
  }

}
