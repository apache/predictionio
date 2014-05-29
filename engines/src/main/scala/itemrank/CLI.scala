package io.prediction.engines.itemrank

import io.prediction.core.{ BaseEngine }
import io.prediction.{ BaseAlgoParams, BaseCleanserParams, BaseServerParams }
import io.prediction.workflow.EvaluationWorkflow

import grizzled.slf4j.Logging
import com.github.nscala_time.time.Imports._

import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.JsonMethods
import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JValue

import org.json4s._

import scala.io.Source

object CLI extends Logging {

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  } ++ JodaTimeSerializers.all

  // run <eval param json> <cleanser param json> <algo param json>
  //     <server param json>
  // run /pio
  def main(args: Array[String]) {

    println(args.mkString(" "))

    // TODO: get class name from command line
    val evaluator = ItemRankEvaluator()
    val engine = ItemRankEngine()

    val evalString = Source.fromFile(args(0)).mkString
    val evalJson = JsonMethods.parse(evalString)
    val evalParams = Extraction.extract(evalJson)(formats,
      evaluator.paramsClass)

    println(evalJson)
    println(evalParams)

    val cleanserString = Source.fromFile(args(1)).mkString
    val cleanserJson = JsonMethods.parse(cleanserString)
    val cleanserParams =
      Extraction.extract(cleanserJson)(formats,
        engine.cleanserClass.newInstance.paramsClass)

    println(cleanserJson)
    println(cleanserParams)

    val algoString = Source.fromFile(args(2)).mkString
    val algoJson = JsonMethods.parse(algoString)
    val algoJsonSeq = algoJson.extract[Seq[Tuple2[String, JValue]]]

    val invalidAlgoIds = algoJsonSeq.filter { case (id, json) =>
      !engine.algorithmClassMap.contains(id)
    }

    if (!invalidAlgoIds.isEmpty) {
      println(s"Invalid algo id defined: ${invalidAlgoIds}")
      System.exit(1)
    }

    val algoParamSet = algoJsonSeq
      .map{ case (id, json) =>
        val p = Extraction.extract(json)(formats,
          engine.algorithmClassMap(id).newInstance.paramsClass)
        (id, p)//.asInstanceOf[BaseAlgoParams])
      }

    println(algoJson)
    println(algoParamSet)

    val serverString = Source.fromFile(args(3)).mkString
    val serverJson = JsonMethods.parse(serverString)

    val serverParams = Extraction.extract(serverJson)(formats,
      engine.serverClass.newInstance.paramsClass)

    println(serverJson)
    println(serverParams)
/*
    val evalWorkflow1 = EvaluationWorkflow(
      "", evalParams,
      cleanserParams.asInstanceOf[BaseCleanserParams],
      algoParamSet,
      serverParams.asInstanceOf[BaseServerParams],
      engine, evaluator.getClass)

    evalWorkflow1.run
*/
  }
}
