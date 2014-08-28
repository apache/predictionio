package io.prediction.examples.elasticsearch

import io.prediction.controller._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import org.apache.hadoop.io.Text
import org.apache.hadoop.conf.Configuration
import org.elasticsearch.hadoop.mr.EsInputFormat

case class DataSource
  extends PDataSource[EmptyParams, AnyRef, AnyRef, AnyRef, AnyRef] {

  def read(sc: SparkContext): Seq[(AnyRef, AnyRef, RDD[(AnyRef, AnyRef)])] = {
    println("Test")

    val conf = new Configuration()
    conf.set("es.resource", "bank/account")
    conf.set("es.query", "?q=*")

    val esRDD = sc.newAPIHadoopRDD(
      conf, 
      classOf[EsInputFormat[Text, Text]],
      classOf[Text], 
      classOf[Text])

    esRDD.take(10).foreach(println)

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

