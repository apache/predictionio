package io.prediction.tools

//import io.prediction.core.{ AbstractEvaluator, AbstractEngine }
import io.prediction.core.{ BaseEvaluator, BaseEngine }
import io.prediction.{ EngineFactory, EvaluatorFactory }
import io.prediction.BaseParams
/*
import io.prediction.{
  BaseAlgoParams,
  BaseCleanserParams,
  BaseServerParams
}
*/
import io.prediction.storage.Config
import io.prediction.storage.Run
//import io.prediction.workflow.EvaluationWorkflow
import io.prediction.workflow.SparkWorkflow
//import io.prediction.storage.Config
//import io.prediction.storage.EngineManifest

import grizzled.slf4j.Logging
import com.github.nscala_time.time.Imports._
import com.twitter.chill.KryoInjection
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.JsonMethods
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

import scala.io.Source
import scala.reflect.runtime.universe

//import java.io.File
//import java.net.URLClassLoader

/*
Example

ItemRank:
run --evaluatorFactory io.prediction.engines.itemrank.ItemRankEvaluator --engineFactory io.prediction.engines.itemrank.ItemRankEngine --dp dataPrepParams.json --vp dataPrepParams.json --cp cleanserParams.json --ap algoParamArray.json --sp serverParams.json --jsonDir src/main/scala/itemrank/examples/

Stock:
run --evaluatorFactory io.prediction.engines.stock.StockEvaluator --engineFactory io.prediction.engines.stock.StockEngine --dp dataPrepParams.json --sp serverParams.json --ap algoParamArray.json --jsonDir src/main/scala/stock/examples/
*/

case class AlgoParams(name: String, params: JValue)

object CreateEvaluationWorkflow extends Logging {

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  } ++ JodaTimeSerializers.all

  case class Args(
    batch: String = "",
    engineManifestId: String = "",
    engineManifestVersion: String = "",
    evaluatorFactoryName: String = "",
    engineFactoryName: String = "",
    dataPrepJsonPath: String = "",
    validatorJsonPath: String = "",
    cleanserJsonPath: String = "",
    algoJsonPath: String = "",
    serverJsonPath: String = "",
    jsonDir: String = ""
  )

  def getParams[A <: AnyRef](jsonDir: String, path: String,
    classManifest: Manifest[A]): A = {
    val jsonString = (
      if (path == "") ""
      else Source.fromFile(jsonDir + path).mkString)
  
    try {
      val json = JsonMethods.parse(jsonString)
      info(json)
      val params = Extraction.extract(json)(formats, classManifest)
      info(params)
      params
    } catch {
      case me: MappingException => {
        println(s"Error reading parameter $classManifest from file $path.")
        throw me
      }
    }
  }

  def main(args: Array[String]) {
    // If json path is not provided, it is assumed to be an empty string.
    val parser = new scopt.OptionParser[Args]("CreateEvaluationWorkflow") {
      head("CreateEvaluationWorkflow", "0.x")
      help("help") text ("prints this usage text")
      opt[String]("engineManifestId") required() valueName("engine ID") action { (x, c) =>
        c.copy(engineManifestId = x)
      }
      opt[String]("engineManifestVersion") required() valueName("engine version") action { (x, c) =>
        c.copy(engineManifestVersion = x)
      }
      opt[String]("batch") action { (x, c) =>
        c.copy(batch = x)
      } text("batch label of the run")
      opt[String]("evaluatorFactory").required()
        .valueName("<evalutor factory name>").action { (x, c) =>
          c.copy(evaluatorFactoryName = x)}
      opt[String]("engineFactory").required().
        valueName("<engine factory name>").action { (x, c) =>
          c.copy(engineFactoryName = x)}
      opt[String]("dp").optional()
        .valueName("<dataprep param json>").action { (x,c) =>
          c.copy(dataPrepJsonPath = x)}
      opt[String]("vp").optional()
        .valueName("<validator param json>").action { (x,c) =>
          c.copy(validatorJsonPath = x)}
      opt[String]("cp").optional()
        .valueName("<cleanser param json>").action { (x,c) =>
          c.copy(cleanserJsonPath = x)}
      opt[String]("ap").optional()
        .valueName("<algo param json>").action { (x,c) =>
          c.copy(algoJsonPath = x) }
      opt[String]("sp").optional()
        .valueName("<server param json>").action { (x,c) =>
          c.copy(serverJsonPath = x) }
      opt[String]("jsonDir").optional()
        .valueName("<json directory>").action { (x,c) =>
          c.copy(jsonDir = x) }
    }

    val arg: Option[Args] = parser.parse(args, Args())

    if (arg == None) {
      error("Invalid arguments")
      System.exit(1)
    }

    //val config = new Config
    //val engineManifests = config.getSettingsEngineManifests
    //val engineManifest = engineManifests.get(arg.get.engineManifestId, arg.get.engineManifestVersion).get

    val evaluatorFactoryName = arg.get.evaluatorFactoryName
    val engineFactoryName = arg.get.engineFactoryName
    //val evaluatorFactoryName = engineManifest.evaluatorFactory
    //val engineFactoryName = engineManifest.engineFactory
    val dataPrepJsonPath = arg.get.dataPrepJsonPath
    val validatorJsonPath = arg.get.validatorJsonPath
    val cleanserJsonPath = arg.get.cleanserJsonPath
    val algoJsonPath = arg.get.algoJsonPath
    val serverJsonPath = arg.get.serverJsonPath
    val jsonDir = arg.get.jsonDir

    info(args.mkString(" "))

    //val engineJarFile = new File(engineManifest.jar)
    //info(s"Engine JAR file (${engineManifest.jar}) exists? ${engineJarFile.exists()}")
    //val classLoader = new URLClassLoader(Array(engineJarFile.toURI.toURL))
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    //val runtimeMirror = universe.runtimeMirror(classLoader)

    // create evaluator instance
    val evaluatorModule = runtimeMirror.staticModule(evaluatorFactoryName)
    val evaluatorObject = runtimeMirror.reflectModule(evaluatorModule)
    val evaluator = evaluatorObject.instance.asInstanceOf[EvaluatorFactory]()

    // create engine instance
    val engineModule = runtimeMirror.staticModule(engineFactoryName)
    val engineObject = runtimeMirror.reflectModule(engineModule)
    val engine = engineObject.instance.asInstanceOf[EngineFactory]()

    // Params
    val dataPrepParams = getParams(
      jsonDir, dataPrepJsonPath,
      evaluator.dataPreparatorClass.newInstance.paramsClass)

    val validatorParams = getParams(
      jsonDir, validatorJsonPath,
      evaluator.validatorClass.newInstance.paramsClass)

    val cleanserParams = getParams(
      jsonDir, cleanserJsonPath,
      engine.cleanserClass.newInstance.paramsClass)

    val serverParams = getParams(
      jsonDir, serverJsonPath,
      engine.serverClass.newInstance.paramsClass)

    // AlgoParams require special handling as it is a Map.
    val algoString = Source.fromFile(jsonDir + algoJsonPath).mkString
    val algoJson = JsonMethods.parse(algoString)
    println(algoJson)
    var algoJsonSeq = algoJson.extract[Seq[AlgoParams]]

    val invalidAlgoIds = algoJsonSeq.filter { ap =>
      !engine.algorithmClassMap.contains(ap.name)
    }

    if (!invalidAlgoIds.isEmpty) {
      error(s"Invalid algo id defined: ${invalidAlgoIds}")
      System.exit(1)
    }

    val algoParamSet = algoJsonSeq
      .map{ ap =>
        val p = Extraction.extract(ap.params)(formats,
          engine.algorithmClassMap(ap.name).newInstance.paramsClass)
        //(ap.name, p)//.asInstanceOf[BaseAlgoParams])
        //(ap.name, p.asInstanceOf[BaseAlgoParams])
        (ap.name, p.asInstanceOf[BaseParams])
      }

    info(algoJson)
    info(algoParamSet)

    val config = new Config
    val runs = config.getSettingsRuns

    // FIXME. Use SparkWorkflow
    //val evalWorkflow1 = EvaluationWorkflow(
    val starttime = DateTime.now
    val evalWorkflow1 = SparkWorkflow.run(
      arg.get.batch,  // Batch Name
      dataPrepParams,
      validatorParams,
      cleanserParams,
      algoParamSet,
      serverParams,
      engine,
      evaluator)
    val endtime = DateTime.now

    runs.insert(Run(
      id = "",
      startTime = starttime,
      endTime = endtime,
      engineManifestId = arg.get.engineManifestId,
      engineManifestVersion = arg.get.engineManifestVersion,
      batch = arg.get.batch,
      evaluationDataParams = write(dataPrepParams),
      validationParams = write(validatorParams),
      cleanserParams = write(cleanserParams),
      algoParamsList = write(algoParamSet.map(t => Map("name" -> t._1, "params" -> t._2))),
      serverParams = write(serverParams),
      models = KryoInjection(evalWorkflow1._1),
      crossValidationResults = write(evalWorkflow1._3)
    ))

    //evalWorkflow1.run
    //*/

  }
}
