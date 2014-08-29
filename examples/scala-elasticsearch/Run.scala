package io.prediction.examples.elasticsearch

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import org.apache.hadoop.io.Text
import org.apache.hadoop.io.MapWritable
import org.apache.hadoop.io.Writable
import org.apache.hadoop.io._
import org.apache.hadoop.conf.Configuration
import org.elasticsearch.hadoop.mr.EsInputFormat

import _root_.java.util.{ Set => JSet }
import _root_.java.util.Map.{ Entry => JMapEntry }

import scala.collection.JavaConversions._

import com.google.gson.Gson

import io.prediction.storage.ItemTrend
import io.prediction.storage.stock.DailyTuple
import com.github.nscala_time.time.Imports._
import io.prediction.storage.Utils

object ESItemTrend {
  def test(mw: MapWritable): Seq[Text] = {
    /*
    mw.entrySet.map { _.getKey.toString }.mkString(" ")
    val seed = ItemTrend(
      id = "",
      appid = 0,
      ct = DateTime.now)
    mw.entrySet.foldLeft(seed) { case (itemtrend, field) => {
      val (key, value) = (field.getKey, field.getValue)
      key match {
        case "appid" => itemtrend.copy(appid = value.toInt)
        case x => itemtrend
      }
    }}
    */
    mw.entrySet.map { _.getKey.asInstanceOf[Text] }.toSeq
  }

  def get(mw: MapWritable): ItemTrend = {
    val seed = ItemTrend(
      id = "",
      appid = 0,
      ct = DateTime.now)

    mw.entrySet.foldLeft(seed) { case (itemtrend, field) => {
      val key: String = field.getKey.asInstanceOf[Text].toString
      val value: Writable = field.getValue
      key match {
        case "appid" => {
          itemtrend.copy(appid = value.asInstanceOf[LongWritable].get.toInt)
        }
        case "id" => {
          itemtrend.copy(id = value.asInstanceOf[Text].toString)
        }
        case "ct" => {
          val ctStr: String = value.asInstanceOf[Text].toString
          val ct: DateTime = Utils.stringToDateTime(ctStr)
          itemtrend.copy(ct = ct)
        }
        case "daily" => {
          val array: Array[Writable] = value.asInstanceOf[ArrayWritable].get
          val daily = array.map { d => {
            val m: Map[String, Writable] = d.asInstanceOf[MapWritable]
              .entrySet
              .map(kv => (kv.getKey.toString, kv.getValue))
              .toMap
            new DailyTuple(
              Utils.stringToDateTime(m("date").asInstanceOf[Text].toString),
              m("open").asInstanceOf[DoubleWritable].get,
              m("high").asInstanceOf[DoubleWritable].get,
              m("low").asInstanceOf[DoubleWritable].get,
              m("close").asInstanceOf[DoubleWritable].get,
              m("volume").asInstanceOf[DoubleWritable].get,
              m("adjclose").asInstanceOf[DoubleWritable].get,
              m("active").asInstanceOf[BooleanWritable].get)
          }}
          itemtrend.copy(daily = daily)
        }
        case x => itemtrend
      }
    }}
  }
}


case class DataSource
  extends PDataSource[EmptyParams, AnyRef, AnyRef, AnyRef, AnyRef] {

  def read(sc: SparkContext): Seq[(AnyRef, AnyRef, RDD[(AnyRef, AnyRef)])] = {
    println("Test")

    val conf = new Configuration()
    conf.set("es.resource", "predictionio_appdata/itemtrends")
    conf.set("es.query", "?q=appid:1008&size=1")

    val esRDD = sc.newAPIHadoopRDD(
      conf, 
      classOf[EsInputFormat[Text, MapWritable]],
      classOf[Text], 
      classOf[MapWritable])
      

    println("X")
    //esRDD.map(_._2.toString).collect.foreach(println)
   
    println("Y")
    //esRDD.map(_._2.entrySet.toString).collect.foreach(println)
    //esRDD.map(e => ESItemTrend.get(e._2.entrySet)).collect.foreach(println)
    esRDD
      .map(e => ESItemTrend.get(e._2))
      .map(e => (e.id, e.daily.size))
      //.map(_.map(_.toString).mkString(","))
      .take(10)
      .foreach(println)

    //println("Z")
    //esRDD.map(e => e._2.toString).take(20).foreach(println)

    //esRDD.map(e => new Gson().toJson(e._2.entrySet)).take(20).foreach(println)

    Seq[(AnyRef, AnyRef, RDD[(AnyRef, AnyRef)])]()
  }
}

object Run {
  def main(args: Array[String]) {
    Workflow.run(
      dataSourceClassOpt = Some(classOf[DataSource]),
      params = WorkflowParams(
        batch = "ES Test",
        verbose = 3)
    )
  }
}

