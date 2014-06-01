package io.prediction.engines.tools

import io.prediction.core.{ AbstractEvaluator, AbstractEngine }
import io.prediction.{ EngineFactory, EvaluatorFactory }
import io.prediction.{ 
  BaseAlgoParams, 
  BaseCleanserParams, 
  BaseServerParams
}
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
Example
run --evaluatorFactory io.prediction.engines.itemrank.ItemRankEvaluator --engineFactory io.prediction.engines.itemrank.ItemRankEngine --dp src/main/scala/itemrank/examples/dataPrepParams.json --vp src/main/scala/itemrank/examples/dataPrepParams.json --cp src/main/scala/itemrank/examples/cleanserParams.json --ap src/main/scala/itemrank/examples/algoParamArray.json --sp src/main/scala/itemrank/examples/serverParams.json

*/

object CreateEvaluationWorkFlow extends Logging {

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  } ++ JodaTimeSerializers.all

  case class Args(
    evaluatorFactoryName: String = "",
    engineFactoryName: String = "",
    //evalJsonPath: String = "",
    dataPrepJsonPath: String = "",
    validatorJsonPath: String = "",
    cleanserJsonPath: String = "",
    algoJsonPath: String = "",
    serverJsonPath: String = ""
  )

  def main(args: Array[String]) {

    val parser = new scopt.OptionParser[Args]("CreateEvaluationWorkFlow") {
      head("CreateEvaluationWorkFlow", "0.x")
      help("help") text ("prints this usage text")
      opt[String]("evaluatorFactory").required()
        .valueName("<evalutor factory name>").action { (x, c) =>
          c.copy(evaluatorFactoryName = x)}
      opt[String]("engineFactory").required().
        valueName("<engine factory name>").action { (x, c) =>
          c.copy(engineFactoryName = x)}
      opt[String]("dp").required()
        .valueName("<dataprep param json>").action { (x,c) =>
          c.copy(dataPrepJsonPath = x)}
      opt[String]("vp").required()
        .valueName("<validator param json>").action { (x,c) =>
          c.copy(validatorJsonPath = x)}
      opt[String]("cp").required()
        .valueName("<cleanser param json>").action { (x,c) =>
          c.copy(cleanserJsonPath = x)}
      opt[String]("ap").required()
        .valueName("<algo param json>").action { (x,c) =>
          c.copy(algoJsonPath = x) }
      opt[String]("sp").required()
        .valueName("<server param json>").action { (x,c) =>
          c.copy(serverJsonPath = x) }
    }

    val arg: Option[Args] = parser.parse(args, Args())

    if (arg == None) {
      error("Invalid arguments")
      System.exit(1)
    }

    val evaluatorFactoryName = arg.get.evaluatorFactoryName
    val engineFactoryName = arg.get.engineFactoryName
    val dataPrepJsonPath = arg.get.dataPrepJsonPath
    val validatorJsonPath = arg.get.validatorJsonPath
    val cleanserJsonPath = arg.get.cleanserJsonPath
    val algoJsonPath = arg.get.algoJsonPath
    val serverJsonPath = arg.get.serverJsonPath

    info(args.mkString(" "))

    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

    // create evaluator instance
    val evaluatorModule = runtimeMirror.staticModule(evaluatorFactoryName)
    val evaluatorObject = runtimeMirror.reflectModule(evaluatorModule)
    val evaluator = evaluatorObject.instance.asInstanceOf[EvaluatorFactory]()

    // create engine instance
    val engineModule = runtimeMirror.staticModule(engineFactoryName)
    val engineObject = runtimeMirror.reflectModule(engineModule)
    val engine = engineObject.instance.asInstanceOf[EngineFactory]()

    val dataPrepString = Source.fromFile(dataPrepJsonPath).mkString
    val dataPrepJson = JsonMethods.parse(dataPrepString)
    val dataPrepParams = Extraction.extract(dataPrepJson)(formats,
      evaluator.dataPreparatorClass.newInstance.paramsClass)

    info(dataPrepJson)
    info(dataPrepParams)
    
    val validatorString = Source.fromFile(validatorJsonPath).mkString
    val validatorJson = JsonMethods.parse(validatorString)
    val validatorParams = Extraction.extract(validatorJson)(formats,
      evaluator.validatorClass.newInstance.paramsClass)

    info(validatorJson)
    info(validatorParams)

    val cleanserString = Source.fromFile(cleanserJsonPath).mkString
    val cleanserJson = JsonMethods.parse(cleanserString)
    val cleanserParams =
      Extraction.extract(cleanserJson)(formats,
        engine.cleanserClass.newInstance.paramsClass)

    info(cleanserJson)
    info(cleanserParams)

    val algoString = Source.fromFile(algoJsonPath).mkString
    val algoJson = JsonMethods.parse(algoString)
    val algoJsonSeq = algoJson.extract[Seq[Tuple2[String, JValue]]]

    val invalidAlgoIds = algoJsonSeq.filter { case (id, json) =>
      !engine.algorithmClassMap.contains(id)
    }

    if (!invalidAlgoIds.isEmpty) {
      error(s"Invalid algo id defined: ${invalidAlgoIds}")
      System.exit(1)
    }

    val algoParamSet = algoJsonSeq
      .map{ case (id, json) =>
        val p = Extraction.extract(json)(formats,
          engine.algorithmClassMap(id).newInstance.paramsClass)
        (id, p)//.asInstanceOf[BaseAlgoParams])
      }

    info(algoJson)
    info(algoParamSet)

    val serverString = Source.fromFile(serverJsonPath).mkString
    val serverJson = JsonMethods.parse(serverString)

    val serverParams = Extraction.extract(serverJson)(formats,
      engine.serverClass.newInstance.paramsClass)

    info(serverJson)
    info(serverParams)

    val evalWorkflow1 = EvaluationWorkflow(
      "",  // Batch Name
      dataPrepParams,
      validatorParams,
      cleanserParams,
      algoParamSet,
      serverParams,
      engine, 
      evaluator)

    evalWorkflow1.run

  }
}
