package io.prediction.engines.tools

import io.prediction.core.{ AbstractEvaluator, AbstractEngine }
import io.prediction.{ EngineFactory, EvaluatorFactory }
import io.prediction.{ BaseAlgoParams, BaseCleanserParams, BaseServerParams,
  BaseEvaluationParams }
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
import scala.reflect.runtime.universe

/* 
run <Evaluator Factory name> <Engine Factory name> <eval param.json>
<cleanser param.json>  <algo params array.json> <server paran.json>

Example
run io.prediction.engines.itemrank.ItemRankEvaluator io.prediction.engines.itemrank.ItemRankEngine  src/main/scala/itemrank/examples/evalParams.json src/main/scala/itemrank/examples/cleanserParams.json  src/main/scala/itemrank/examples/algoParamArray.json  src/main/scala/itemrank/examples/serverParams.json
*/

object CreateEvaluationWorkFlow extends Logging {

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  } ++ JodaTimeSerializers.all

  // run <eval param json> <cleanser param json> <algo param json>
  //     <server param json>
  // run /pio
  def main(args: Array[String]) {

    val evaluatorFactoryName = args(0)
    val engineFactoryName = args(1)
    val evalJsonPath = args(2)
    val cleanserJsonPath = args(3)
    val algoJsonPath = args(4)
    val serverJsonPath = args(5)

    println(args.mkString(" "))

    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

    // create evaluator instance
    val evaluatorModule = runtimeMirror.staticModule(evaluatorFactoryName)
    val evaluatorObject = runtimeMirror.reflectModule(evaluatorModule)
    val evaluator = evaluatorObject.instance.asInstanceOf[EvaluatorFactory]()

    // create engine instance
    val engineModule = runtimeMirror.staticModule(engineFactoryName)
    val engineObject = runtimeMirror.reflectModule(engineModule)
    val engine = engineObject.instance.asInstanceOf[EngineFactory]()

    val evalString = Source.fromFile(evalJsonPath).mkString
    val evalJson = JsonMethods.parse(evalString)
    val evalParams = Extraction.extract(evalJson)(formats,
      evaluator.paramsClass)

    println(evalJson)
    println(evalParams)

    val cleanserString = Source.fromFile(cleanserJsonPath).mkString
    val cleanserJson = JsonMethods.parse(cleanserString)
    val cleanserParams =
      Extraction.extract(cleanserJson)(formats,
        engine.cleanserClass.newInstance.paramsClass)

    println(cleanserJson)
    println(cleanserParams)

    val algoString = Source.fromFile(algoJsonPath).mkString
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

    val serverString = Source.fromFile(serverJsonPath).mkString
    val serverJson = JsonMethods.parse(serverString)

    val serverParams = Extraction.extract(serverJson)(formats,
      engine.serverClass.newInstance.paramsClass)

    println(serverJson)
    println(serverParams)

    val evalWorkflow1 = EvaluationWorkflow(
      "", evalParams,
      cleanserParams,
      algoParamSet,
      serverParams,
      engine, evaluator.getClass)

    evalWorkflow1.run

  }
}
