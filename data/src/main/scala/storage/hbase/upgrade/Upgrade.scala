package io.prediction.data.storage.hbase.upgrade

import io.prediction.data.storage.Storage
import io.prediction.data.storage.hbase.HBLEvents
import io.prediction.data.storage.hbase.HBEventsUtil

import scala.collection.JavaConversions._

/* Experimental */
object Upgrade {

  def main(args: Array[String]) {
    val fromAppId = args(0).toInt
    val toAppId = args(1).toInt
    val batchSize = args.lift(2).map(_.toInt).getOrElse(100)
    val fromNamespace = args.lift(3).getOrElse("predictionio_eventdata")

    upgrade(fromAppId, toAppId, batchSize, fromNamespace)
  }

  /* For upgrade from 0.8.0 or 0.8.1 to 0.8.2 only*/
  def upgrade(
    fromAppId: Int,
    toAppId: Int,
    batchSize: Int,
    fromNamespace: String) {

    val events = Storage.getLEvents().asInstanceOf[HBLEvents]

    // Assume already run "pio app new <newapp>" (new app already created)
    // TODO: check if new table empty and warn user if not
    val newTable = events.getTable(toAppId)

    val newTableName = newTable.getName().getNameAsString()
    println(s"Copying data from ${fromNamespace}:events for app ID ${fromAppId}"
      + s" to new HBase table ${newTableName}...")

    HB_0_8_0.getByAppId(
      events.client.connection,
      fromNamespace,
      fromAppId).grouped(batchSize).foreach { eventGroup =>
        val puts = eventGroup.map{ e =>
          val (put, rowkey) = HBEventsUtil.eventToPut(e, toAppId)
          put
        }
        newTable.put(puts.toList)
      }

    newTable.flushCommits()
    newTable.close()
    println("Done.")
  }

}
