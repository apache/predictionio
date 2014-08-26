package io.prediction.dataapi.hbase

import io.prediction.dataapi.Event
import io.prediction.dataapi.StorageError
import io.prediction.dataapi.Events

import grizzled.slf4j.Logging

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{ read, write }
import org.json4s.ext.JodaTimeSerializers

import com.github.nscala_time.time.Imports._

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
import scala.concurrent.ExecutionContext.Implicits.global // TODO

import java.util.UUID

class HBEvent(client: HBClient, namespace: String) extends Events with Logging {

  implicit val formats = DefaultFormats.lossless ++ JodaTimeSerializers.all

  val tableName = TableName.valueOf(namespace, "events")
  val table = new HTable(client.conf, tableName)

  // create table if not exist
  if (!client.admin.tableExists(tableName)) {
    val tableDesc = new HTableDescriptor(tableName)
    tableDesc.addFamily(new HColumnDescriptor("e")) // e:tid
    tableDesc.addFamily(new HColumnDescriptor("p"))
    tableDesc.addFamily(new HColumnDescriptor("tag")) // tag
    tableDesc.addFamily(new HColumnDescriptor("o")) // others
    client.admin.createTable(tableDesc)
  }

  private def eventToRowKey(event: Event): String = {
    // TODO: could be bad since writing to same region for same appId?
    // TODO: hash entityId and event to avoid arbitaray string length
    // and conflict with delimiter
    val uuid: Long = UUID.randomUUID().getLeastSignificantBits
    event.appId + "-" + event.eventTime.getMillis + "-" +
      event.event + "-" + event.entityId + "-" + uuid
  }

  private def appIdToStartStopRowKey(appId: Int): (String, String) = {
    (appId + "-", (appId+1) + "-")
  }

  private def rowKeyToPartialEvent(rowKey: String): Event = {
    val data = rowKey.split("-")

    Event(
      entityId = data(3),
      targetEntityId = None, // partial
      event = data(2),
      properties = JObject(List()), // partial
      eventTime = new DateTime(data(1).toLong),
      tags = Seq(), // partial
      appId = data(0).toInt,
      predictionKey = None // partial
    )
  }

  private def rowKeyToEventId(rowKey: String): String = rowKey

  private def eventIdToRowKey(eventId: String): String = eventId

  def futureInsert(event: Event): Future[Either[StorageError, String]] = {
    Future {
      val table = new HTable(client.conf, tableName)
      val rowKey = eventToRowKey(event)
      val put = new Put(Bytes.toBytes(rowKey))
      if (event.targetEntityId != None) {
        put.add(Bytes.toBytes("e"), Bytes.toBytes("tid"),
          Bytes.toBytes(event.targetEntityId.get))
      }
      // TODO: better way to handle event.properties?
      // serialize whole properties as string for now..
      put.add(Bytes.toBytes("p"), Bytes.toBytes("p"),
        Bytes.toBytes(write(event.properties)))
      event.tags.foreach { tag =>
        put.add(Bytes.toBytes("tag"), Bytes.toBytes(tag), Bytes.toBytes(true))
      }
      if (event.predictionKey != None) {
        put.add(Bytes.toBytes("o"), Bytes.toBytes("pk"),
          Bytes.toBytes(event.predictionKey.get))
      }
      table.put(put)
      table.flushCommits()
      table.close()
      Right(rowKeyToEventId(rowKey))
    }
  }

  private def resultToEvent(result: Result): Event = {
    val rowKey = Bytes.toString(result.getRow())

    val e = result.getFamilyMap(Bytes.toBytes("e"))
    val p = result.getFamilyMap(Bytes.toBytes("p"))
    val tag = result.getFamilyMap(Bytes.toBytes("tag"))
    val o = result.getFamilyMap(Bytes.toBytes("o"))

    val targetEntityId = if (e != null) {
      val tid = e.get(Bytes.toBytes("tid"))
      if (tid != null) Some(Bytes.toString(tid)) else None
    } else None

    val properties = read[JObject](Bytes.toString(p.get(Bytes.toBytes("p"))))

    val tags = if (tag != null)
      tag.keySet.toSeq.map(Bytes.toString(_))
    else Seq()

    val predictionKey = if (o != null) {
      val pk = o.get(Bytes.toBytes("pk"))
      if (pk != null) Some(Bytes.toString(pk)) else None
    } else None

    val partialEvent = rowKeyToPartialEvent(rowKey)
    val event = partialEvent.copy(
      targetEntityId = targetEntityId,
      properties = properties,
      tags = tags,
      predictionKey = predictionKey
    )
    event
  }

  def futureGet(eventId: String):
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

  def futureDelete(eventId: String): Future[Either[StorageError, Boolean]] = {
    Future {
      val rowKeyBytes = Bytes.toBytes(eventIdToRowKey(eventId))
      val exists = table.exists(new Get(rowKeyBytes))
      table.delete(new Delete(rowKeyBytes))
      Right(exists)
    }
  }

  def futureGetByAppId(appId: Int):
    Future[Either[StorageError, Iterator[Event]]] = {
      Future {
        val (start, stop) = appIdToStartStopRowKey(appId)
        val scan = new Scan(Bytes.toBytes(start), Bytes.toBytes(stop))
        val scanner = table.getScanner(scan)
        Right(scanner.iterator().map { resultToEvent(_) })
      }
    }

  def futureDeleteByAppId(appId: Int): Future[Either[StorageError, Unit]] = {
    Future {
      // TODO: better way to handle range delete
      val (start, stop) = appIdToStartStopRowKey(appId)
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

object HBEventTests {

  def main (args: Array[String]) {
    //testEmptyPut()
    test()
    //testHBEvent()
  }

  def testEmptyPut() = {
    import org.apache.hadoop.hbase.HBaseConfiguration
    import org.apache.hadoop.hbase.client.HBaseAdmin

    println("test")

    val conf = HBaseConfiguration.create();
    val admin = new HBaseAdmin(conf)

    val listtables = admin.listTables()
    listtables.foreach(println)

    val tableName = "newtable"
    if (!admin.tableExists(tableName)) {
      // create table
      val tableDesr = new HTableDescriptor(TableName.valueOf(tableName))
      tableDesr.addFamily(new HColumnDescriptor("cf1"))
      tableDesr.addFamily(new HColumnDescriptor("cf2"))
      admin.createTable(tableDesr)
    }


    val table = new HTable(conf, tableName)

    val p2 = new Put(Bytes.toBytes("rowEMPTY"))
    table.put(p2)
    table.flushCommits()
    table.close()

  }

  def test() = {
    import org.apache.hadoop.hbase.HBaseConfiguration
    import org.apache.hadoop.hbase.client.HBaseAdmin

    println("test")

    val conf = HBaseConfiguration.create();
    val admin = new HBaseAdmin(conf)

    val listtables = admin.listTables()
    listtables.foreach(println)

    val tableName = "newtable"
    if (!admin.tableExists(tableName)) {
      // create table
      val tableDesr = new HTableDescriptor(TableName.valueOf(tableName))
      tableDesr.addFamily(new HColumnDescriptor("cf1"))
      tableDesr.addFamily(new HColumnDescriptor("cf2"))
      admin.createTable(tableDesr)
    }

    val table = new HTable(conf, tableName)
    val p = new Put(Bytes.toBytes("row1"))
    p.add(Bytes.toBytes("cf1"), Bytes.toBytes("qa1"),
      Bytes.toBytes("value1"))
    p.add(Bytes.toBytes("cf1"), Bytes.toBytes("qa2"),
      Bytes.toBytes("value2"))
    p.add(Bytes.toBytes("cf2"), Bytes.toBytes("qaA"),
      Bytes.toBytes("valueA"))
    table.put(p)
    table.flushCommits()
    table.close()

    val g = new Get(Bytes.toBytes("row1"))
    g.addFamily(Bytes.toBytes("cf1"))
    g.setMaxVersions(3)
    val result = table.get(g)
    println(result.getExists())
    println(result.isEmpty())
    println(result)
    val m = result.getFamilyMap(Bytes.toBytes("cf1"))
    println(m.get(Bytes.toBytes("qa1")))
    println(m.get(Bytes.toBytes("qaX")))
    println(result.getFamilyMap(Bytes.toBytes("cfx")))


    val result2 = table.get(new Get(Bytes.toBytes("rowX")))
    println(result2.getExists())
    println(result2.isEmpty())
    println(result2.getMap())
    println(result2)

    val g4 = new Get(Bytes.toBytes("row1"))
    val result4 = table.get(g4)
    println(result4)

    val d = new Delete(Bytes.toBytes("row1"))
    table.delete(d)

    admin.close()
  }

  def testHBEvent() = {
    import io.prediction.dataapi.StorageClientConfig

    println("testHBEvent")

    val e = Event(
      entityId = "abc",
      targetEntityId = None,
      event = "$set",
      properties = parse("""
        { "numbers" : [1, 2, 3, 4],
          "abc" : "some_string",
          "def" : 4, "k" : false
        } """).asInstanceOf[JObject],
      eventTime = DateTime.now,
      tags = List("tag1", "tag2"),
      appId = 4,
      predictionKey = None
    )

    val config = StorageClientConfig(Seq("localhost"), Seq(9300))
    val storageClient = new HBStorageClient(config)
    val client = storageClient.client
    val eventConnector = storageClient.eventClient

    val de = eventConnector.insert(e)
    println(de)
    de match {
      case Right(d) => {
        val e2 = eventConnector.get(d)
        println(e2)
        val k = eventConnector.delete(d)
        println(k)
        val k2 = eventConnector.delete(d)
        println(k2)
      }
      case _ => {println("match error")}
    }

    val i1 = eventConnector.insert(e)
    println(i1)
    val i2 = eventConnector.insert(e)
    println(i2)
    val i3 = eventConnector.insert(e)
    println(i3)

    val all = eventConnector.getByAppId(4)
    println(all.right.map{ x =>
      val l = x.toList
      s"size ${l.size}, ${l}"
    })

    val delAll = eventConnector.deleteByAppId(4)
    println(delAll)
    val all2 = eventConnector.getByAppId(4)
    println(all2)

  }

}
