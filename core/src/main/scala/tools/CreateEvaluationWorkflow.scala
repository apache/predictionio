package io.prediction.core.tools

import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import org.json4s.ext.JodaTimeSerializers

import com.github.nscala_time.time.Imports._

object CreateEvaluationWorkflow extends Logging {

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  } ++ JodaTimeSerializers.all

  class DataX(
    val x: Int,
    val y: Int
  ) {
    override def toString = s"${x} ${y}"
  }

  class EvalParams (
    //val iterations: Int
    val appid: Int,
    val itypes: Option[Set[String]],
    // action for training
    val actions: Map[String, Option[Int]], // ((view, 1), (rate, None))
    val conflict: Int = 10,
    val trainStart: DateTime,
    val x: DataX
  ) {
    override def toString = s"${appid}, ${itypes}, ${actions} ${conflict} ${trainStart} ${x}"
  }

  def main(args: Array[String]): Unit = {
    val a = parse(""" {
      "appid": 1,
      "itypes": ["type1", "type2"],
      "actions": { "view": 1, "like" : 4 },
      "conflict": "abc",
      "trainStart" : "2014-04-01T00:00:00.000Z",
      "x": { "x": 123, "y": 567}
     } """)

    val p = a.extract[EvalParams]

    println(p)

  }
}
