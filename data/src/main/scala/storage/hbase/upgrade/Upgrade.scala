package io.prediction.data.storage.hbase.upgrade

import io.prediction.data.storage.Storage
import io.prediction.data.storage.hbase.HBEvents
import io.prediction.data.storage.hbase.HBEventsUtil

import scala.collection.JavaConversions._

/* Experimental */
object Upgrade {

  def main(args: Array[String]) {
    val appId = args(0).toInt
    val batchSize = args.lift(1).map(_.toInt).getOrElse(100)
    val fromNamespace = args.lift(2).getOrElse("predictionio_eventdata")

    upgrade(appId, batchSize, fromNamespace)
  }

  /* For upgrade from 0.8.0 or 0.8.1 to 0.8.2 only*/
  def upgrade(
    appId: Int,
    batchSize: Int,
    fromNamespace: String) {

    val events = Storage.getEventDataEvents().asInstanceOf[HBEvents]

    // TODO: check if new table empty and warn user if not
    events.init(appId)
    val newTable = events.getTable(appId)

    val newTableName = newTable.getName().getNameAsString()
    println(s"Copying data from ${fromNamespace}:events for app ID ${appId}" +
      s" to new HBase table ${newTableName}...")

    HB_0_8_0.getByAppId(
      events.client.connection,
      fromNamespace,
      appId).grouped(batchSize).foreach { eventGroup =>
        val puts = eventGroup.map{ e =>
          val (put, rowkey) = HBEventsUtil.eventToPut(e, appId)
          put
        }
        newTable.put(puts.toList)
      }

    newTable.flushCommits()
    newTable.close()
    println("Done.")
  }

}
