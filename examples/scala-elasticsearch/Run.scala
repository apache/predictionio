package io.prediction.examples.elasticsearch

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import org.apache.hadoop.io.Text
import org.apache.hadoop.io.MapWritable
import org.apache.hadoop.io.Writable
import org.apache.hadoop.conf.Configuration
import org.elasticsearch.hadoop.mr.EsInputFormat

import _root_.java.util.{ Set => JSet }
import _root_.java.util.Map.{ Entry => JMapEntry }

import scala.collection.JavaConversions._

import com.google.gson.Gson

object ESItemTrend {
  def get(mw: MapWritable): String = {
    mw.entrySet.map { _.getKey.toString }.mkString(" ")
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
    esRDD.map(e => ESItemTrend.get(e._2)).take(10).foreach(println)

    println("Z")
    esRDD.map(e => e._2.toString).take(20).foreach(println)

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

