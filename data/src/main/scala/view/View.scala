package io.prediction.data.view

import io.prediction.data.storage.Event
import io.prediction.data.storage.Events
import io.prediction.data.storage.DataMap
import io.prediction.data.storage.Storage

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{ read, write }
import org.json4s.ext.JodaTimeSerializers

import scala.concurrent.ExecutionContext.Implicits.global // TODO

class LBatchView {

  def entityPropertiesView(
    events: Iterator[Event]): Map[String, DataMap] = {

    def eventFilter(e: Event) = (e.event == "$set") || (e.event == "$unset")

    def aggregate(p: DataMap, e: Event): DataMap = {
      e.event match {
        case "$set" => p ++ e.properties
        case "$unset" => p -- e.properties.keySet
      }
    }

    aggregateByEntityOrdered[DataMap](events, eventFilter, DataMap(), aggregate)
  }

  def aggregateByEntityOrdered[T](
    events: Iterator[Event],
    filter: Event => Boolean,
    init: T,
    aggregate: (T, Event) => T): Map[String, T] = {

    events.filter( filter(_) ).toArray
      .groupBy( _.entityId )
      .mapValues( _.sortBy(_.eventTime.getMillis).foldLeft[T](init)(aggregate))
      .toMap

  }

  def groupByEntityOrdered[T](
    events: Iterator[Event],
    filter: Event => Boolean,
    map: Event => T): Map[String, Seq[T]] = {

    events.filter( filter(_) ).toArray
      .groupBy( _.entityId )
      .mapValues( _.sortBy(_.eventTime.getMillis).map(map(_)) )
      .toMap
  }

}

object TestLBatchView {

  def main(args: Array[String]) {
    println("view run")
    val storageType = if (args.isEmpty) "ES" else args(0)
    val eventClient = Storage.getDataObject[Events](storageType)

    test(eventClient)
  }

  def test(eventClient: Events) = {

    val result = eventClient.getByAppId(2)
    val eventArray = result match {
      case Right(x) => x.toArray
      case Left(y) => Array()
    }
    println(eventArray.mkString(","))

    println("properties view:")
    val v = new LBatchView()
    val p = v.entityPropertiesView(eventArray.iterator)
    println(p)

    case class RateAction(
      val iid: String,
      val rate: Int,
      val t: Long
    )

    def map(e: Event): RateAction = {
      RateAction(
        iid = e.targetEntityId.get,
        rate = e.properties.get[Int]("pio_rate"),
        t = e.eventTime.getMillis
      )
    }

    val actions = v.groupByEntityOrdered(
      eventArray.iterator,
      (e => (e.event == "rate")),
      map
    )
    println("acion view:")
    println(actions)

  }

}
