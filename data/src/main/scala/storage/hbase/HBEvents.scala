package io.prediction.data.storage.hbase

import io.prediction.data.storage.Event
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

import scala.collection.JavaConversions._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import java.util.UUID

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
  val table = new HTable(client.conf, tableName)

  // create table if not exist
  if (!client.admin.tableExists(tableName)) {
    val tableDesc = new HTableDescriptor(tableName)
    // general event related
    // e:ttype - target entity type
    // e:tid - target entity id
    // e:p - properties
    // e:pk - predictionkey
    // e:etz - event time zone
    // e:ctz - creation time zone
    tableDesc.addFamily(new HColumnDescriptor("e"))
    tableDesc.addFamily(new HColumnDescriptor("tag")) // tag: tag-name
    client.admin.createTable(tableDesc)
  }

  private def eventToRowKey(event: Event): String = {
    // TODO: could be bad since writing to same region for same appId?
    // TODO: hash entityId and event to avoid arbitaray string length
    // and conflict with delimiter
    val uuid: Long = UUID.randomUUID().getLeastSignificantBits
    Seq(
      event.appId,
      event.eventTime.getMillis,
      event.event,
      event.entityType,
      event.entityId,
      uuid
    ).mkString("-")
  }

  private def startStopRowKey(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime]) = {

    (appId, startTime, untilTime) match {
      case (x, None, None) => (x + "-", (x+1) + "-")
      case (x, Some(start), None) => (x + "-" + start.getMillis + "-",
        (x+1) + "-")
      case (x, None, Some(end)) => (x + "-", x + "-" + end.getMillis + "-")
      case (x, Some(start), Some(end)) =>
        (x + "-" + start.getMillis + "-", x + "-" + end.getMillis + "-")
    }
  }

  private def rowKeyToPartialEvent(rowKey: String): Event = {
    val data = rowKey.split("-")

    Event(
      event = data(2),
      entityType = data(3),
      entityId = data(4),
      eventTime = new DateTime(data(1).toLong), // missing timezone info
      appId = data(0).toInt
    )
  }

  private def rowKeyToEventId(rowKey: String): String = rowKey

  private def eventIdToRowKey(eventId: String): String = eventId

  override
  def futureInsert(event: Event)(implicit ec: ExecutionContext):
    Future[Either[StorageError, String]] = {
    Future {
      val table = new HTable(client.conf, tableName)
      val rowKey = eventToRowKey(event)
      val eBytes = Bytes.toBytes("e")
      val put = new Put(Bytes.toBytes(rowKey), event.creationTime.getMillis)
      if (event.targetEntityType != None) {
        put.add(eBytes, Bytes.toBytes("ttype"),
          Bytes.toBytes(event.targetEntityType.get))
      }
      if (event.targetEntityId != None) {
        put.add(eBytes, Bytes.toBytes("tid"),
          Bytes.toBytes(event.targetEntityId.get))
      }
      if (!event.properties.isEmpty) {
        put.add(eBytes, Bytes.toBytes("p"),
          Bytes.toBytes(write(event.properties.toJObject)))
      }
      event.predictionKey.foreach { pk =>
        put.add(eBytes, Bytes.toBytes("pk"), Bytes.toBytes(pk))
      }
      put.add(eBytes, Bytes.toBytes("etz"),
        Bytes.toBytes(event.eventTime.getZone.getID))
      put.add(eBytes, Bytes.toBytes("ctz"),
        Bytes.toBytes(event.creationTime.getZone.getID))

      // use zero-length byte array for tag cell value
      event.tags.foreach { tag =>
        put.add(Bytes.toBytes("tag"), Bytes.toBytes(tag), Array[Byte]())
      }

      table.put(put)
      table.flushCommits()
      table.close()
      Right(rowKeyToEventId(rowKey))
    }
  }

  private def resultToEvent(result: Result): Event = {
    val rowKey = Bytes.toString(result.getRow())

    val eBytes = Bytes.toBytes("e")
    val e = result.getFamilyMap(eBytes)
    val tag = result.getFamilyMap(Bytes.toBytes("tag"))

    val ttype = e.get(Bytes.toBytes("ttype"))
    val targetEntityType = if (ttype != null) Some(Bytes.toString(ttype))
      else None

    val tid = e.get(Bytes.toBytes("tid"))
    val targetEntityId = if (tid != null) Some(Bytes.toString(tid)) else None

    val p = e.get(Bytes.toBytes("p"))
    val properties: DataMap = if (p != null)
      DataMap(read[JObject](Bytes.toString(p))) else DataMap()

    val pk = e.get(Bytes.toBytes("pk"))
    val predictionKey = if (pk != null) Some(Bytes.toString(pk)) else None

    val etz = e.get(Bytes.toBytes("etz"))
    val eventTimeZone = DateTimeZone.forID(Bytes.toString(etz))

    val ctz = e.get(Bytes.toBytes("ctz"))
    val creationTimeZone = DateTimeZone.forID(Bytes.toString(ctz))

    val ctzCell = result.getColumnLatestCell(eBytes, Bytes.toBytes("ctz"))
    val creationTime: DateTime = new DateTime(
      ctzCell.getTimestamp(), creationTimeZone)

    val tags = if (tag != null)
      tag.keySet.toSeq.map(Bytes.toString(_))
    else Seq()

    val partialEvent = rowKeyToPartialEvent(rowKey)
    val event = partialEvent.copy(
      targetEntityType = targetEntityType,
      targetEntityId = targetEntityId,
      properties = properties,
      tags = tags,
      predictionKey = predictionKey,
      eventTime = partialEvent.eventTime.withZone(eventTimeZone),
      creationTime = creationTime
    )
    event
  }

  override
  def futureGet(eventId: String)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Option[Event]]] = {
      Future {
        val get = new Get(Bytes.toBytes(eventId))

        val result = table.get(get)

        if (!result.isEmpty()) {
          val event = resultToEvent(result)
          Right(Some(event))
        } else {
          Right(None)
        }
      }
    }

  override
  def futureDelete(eventId: String)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Boolean]] = {
    Future {
      val rowKeyBytes = Bytes.toBytes(eventIdToRowKey(eventId))
      val exists = table.exists(new Get(rowKeyBytes))
      table.delete(new Delete(rowKeyBytes))
      Right(exists)
    }
  }

  override
  def futureGetByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      Future {
        val (start, stop) = startStopRowKey(appId, None, None)
        val scan = new Scan(Bytes.toBytes(start), Bytes.toBytes(stop))
        val scanner = table.getScanner(scan)
        Right(scanner.iterator().map { resultToEvent(_) })
      }
    }

  override
  def futureGetByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      Future {
        val (start, stop) = startStopRowKey(appId, startTime, untilTime)
        val scan = new Scan(Bytes.toBytes(start), Bytes.toBytes(stop))
        val scanner = table.getScanner(scan)
        Right(scanner.iterator().map { resultToEvent(_) })
      }
  }

  override
  def futureDeleteByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Unit]] = {
    Future {
      // TODO: better way to handle range delete
      val (start, stop) = startStopRowKey(appId, None, None)
      val scan = new Scan(Bytes.toBytes(start), Bytes.toBytes(stop))
      val scanner = table.getScanner(scan)
      val it = scanner.iterator()
      while (it.hasNext()) {
        val result = it.next()
        table.delete(new Delete(result.getRow()))
      }
      scanner.close()
      Right(())
    }
  }

}
