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
import io.prediction.data.storage.DataMap
import io.prediction.data.storage.LEvents
import io.prediction.data.storage.LEventAggregator
import io.prediction.data.storage.StorageError
import io.prediction.data.storage.hbase.HBEventsUtil.RowKey
import io.prediction.data.storage.hbase.HBEventsUtil.RowKeyException

import grizzled.slf4j.Logging

import org.joda.time.DateTime

import org.apache.hadoop.hbase.NamespaceDescriptor
import org.apache.hadoop.hbase.HTableDescriptor
import org.apache.hadoop.hbase.HColumnDescriptor
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.HTable
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.client.Get
import org.apache.hadoop.hbase.client.Delete
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan

import scala.collection.JavaConversions._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class HBLEvents(val client: HBClient, val namespace: String)
  extends LEvents with Logging {

  //implicit val formats = DefaultFormats + new EventJson4sSupport.DBSerializer

  def resultToEvent(result: Result, appId: Int): Event =
    HBEventsUtil.resultToEvent(result, appId)

  def getTable(appId: Int) = client.connection.getTable(
    HBEventsUtil.tableName(namespace, appId))

  override
  def init(appId: Int): Boolean = {
    // check namespace exist
    val existingNamespace = client.admin.listNamespaceDescriptors()
      .map(_.getName)
    if (!existingNamespace.contains(namespace)) {
      val nameDesc = NamespaceDescriptor.create(namespace).build()
      info(s"The namespace ${namespace} doesn't exist yet. Creating now...")
      client.admin.createNamespace(nameDesc)
    }

    val tableName = TableName.valueOf(HBEventsUtil.tableName(namespace, appId))
    if (!client.admin.tableExists(tableName)) {
      info(s"The table ${tableName.getNameAsString()} doesn't exist yet." +
        " Creating now...")
      val tableDesc = new HTableDescriptor(tableName)
      tableDesc.addFamily(new HColumnDescriptor("e"))
      tableDesc.addFamily(new HColumnDescriptor("r")) // reserved
      client.admin.createTable(tableDesc)
    }
    true
  }

  override
  def remove(appId: Int): Boolean = {
    val tableName = TableName.valueOf(HBEventsUtil.tableName(namespace, appId))
    try {
      if (client.admin.tableExists(tableName)) {
        info(s"Removing table ${tableName.getNameAsString()}...")
        client.admin.disableTable(tableName)
        client.admin.deleteTable(tableName)
      } else {
        info(s"Table ${tableName.getNameAsString()} doesn't exist." +
          s" Nothing is deleted.")
      }
      true
    } catch {
      case e: Exception => {
        error(s"Fail to remove table for appId ${appId}. Exception: ${e}")
        false
      }
    }
  }

  override
  def close() = {
    client.admin.close()
    client.connection.close()
  }

  override
  def futureInsert(event: Event, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, String]] = {
    Future {
      val table = getTable(appId)
      val (put, rowKey) = HBEventsUtil.eventToPut(event, appId)
      table.put(put)
      table.flushCommits()
      table.close()
      Right(rowKey.toString)
    }/*.recover {
      case e: Exception => Left(StorageError(e.toString))
    }*/
  }



  override
  def futureGet(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Option[Event]]] = {
      Future {
        val table = getTable(appId)
        val rowKey = RowKey(eventId)
        val get = new Get(rowKey.toBytes)

        val result = table.get(get)
        table.close()

        if (!result.isEmpty()) {
          val event = resultToEvent(result, appId)
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
  def futureDelete(eventId: String, appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Boolean]] = {
    Future {
      val table = getTable(appId)
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
      futureFind(
        appId = appId,
        startTime = None,
        untilTime = None,
        entityType = None,
        entityId = None,
        eventNames = None,
        limit = None,
        reversed = None)
    }

  override
  def futureGetByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      futureFind(
        appId = appId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = None,
        entityId = None,
        eventNames = None,
        limit = None,
        reversed = None)
  }

  override
  def futureGetByAppIdAndTimeAndEntity(appId: Int,
    startTime: Option[DateTime],
    untilTime: Option[DateTime],
    entityType: Option[String],
    entityId: Option[String])(implicit ec: ExecutionContext):
    Future[Either[StorageError, Iterator[Event]]] = {
      futureFind(
        appId = appId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = entityType,
        entityId = entityId,
        eventNames = None,
        limit = None,
        reversed = None)
  }

  override
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
    Future[Either[StorageError, Iterator[Event]]] = {
      Future {
        val table = getTable(appId)

        val scan = HBEventsUtil.createScan(
          startTime = startTime,
          untilTime = untilTime,
          entityType = entityType,
          entityId = entityId,
          eventNames = eventNames,
          targetEntityType = targetEntityType,
          targetEntityId = targetEntityId,
          reversed = reversed)
        val scanner = table.getScanner(scan)
        table.close()

        val eventsIter = scanner.iterator()

        // Get all events if None or Some(-1)
        val results: Iterator[Result] = limit match {
          case Some(-1) => eventsIter
          case None => eventsIter
          case Some(x) => eventsIter.take(x)
        }

        val eventsIt = results.map { resultToEvent(_, appId) }

        Right(eventsIt)
      }
  }

  override
  def futureAggregateProperties(
    appId: Int,
    entityType: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None,
    required: Option[Seq[String]] = None)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Map[String, DataMap]]] = {
      futureFind(
        appId = appId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = Some(entityType),
        eventNames = Some(LEventAggregator.eventNames)
      ).map{ either =>
        either.right.map{ eventIt =>
          val dm = LEventAggregator.aggregateProperties(eventIt)
          if (required.isDefined) {
            dm.filter { case (k, v) =>
              required.get.map(v.contains(_)).reduce(_ && _)
            }
          } else dm
        }
      }
    }

  override
  def futureAggregatePropertiesSingle(
    appId: Int,
    entityType: String,
    entityId: String,
    startTime: Option[DateTime] = None,
    untilTime: Option[DateTime] = None)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Option[DataMap]]] = {
      futureFind(
        appId = appId,
        startTime = startTime,
        untilTime = untilTime,
        entityType = Some(entityType),
        entityId = Some(entityId),
        eventNames = Some(LEventAggregator.eventNames)
      ).map{ either =>
        either.right.map{ eventIt =>
          LEventAggregator.aggregatePropertiesSingle(eventIt)
        }
      }
    }

  override
  def futureDeleteByAppId(appId: Int)(implicit ec: ExecutionContext):
    Future[Either[StorageError, Unit]] = {
    Future {
      // TODO: better way to handle range delete
      val table = getTable(appId)
      val scan = new Scan()
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
