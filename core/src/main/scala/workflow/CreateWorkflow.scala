package io.prediction.workflow

import io.prediction.controller.EmptyParams
import io.prediction.controller.EngineParams
import io.prediction.controller.IEngineFactory
import io.prediction.controller.Metrics
import io.prediction.controller.Params

import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.io.Source
import scala.reflect.runtime.universe

import java.io.File

object CreateWorkflow extends Logging {

  case class WorkflowConfig(
    batch: String = "Transient Lazy Val",
    engineFactory: String = "",
    metricsClass: Option[String] = None,
    dataSourceParamsJsonPath: Option[String] = None,
    preparatorParamsJsonPath: Option[String] = None,
    algorithmsParamsJsonPath: Option[String] = None,
    servingParamsJsonPath: Option[String] = None,
    metricsParamsJsonPath: Option[String] = None,
    jsonBasePath: String = "",
    env: Option[String] = None)

  case class AlgorithmParams(name: String, params: JValue)

  implicit val formats = DefaultFormats

  private def extractParams(json: String, clazz: Class[_]): Params = {
    val pClass = clazz.getConstructors.head.getParameterTypes
    if (pClass.size == 0) {
      if (json != "")
        warn(s"Non-empty parameters supplied to ${clazz.getName}, but its " +
          "constructor does not accept any arguments. Stubbing with empty " +
          "parameters.")
      EmptyParams()
    } else {
      val apClass = pClass.head
      try {
        Extraction.extract(parse(json), reflect.TypeInfo(apClass, None)).
          asInstanceOf[Params]
      } catch {
        case me: MappingException => {
          error(s"Unable to extract parameters for ${apClass.getName} from " +
            s"JSON string: ${json}. Aborting workflow.")
          sys.exit(1)
        }
      }
    }
  }

  private def stringFromFile(basePath: String, filePath: String): String = {
    try {
      if (basePath == "")
        Source.fromFile(filePath).mkString
      else
        Source.fromFile(basePath + File.separator + filePath).mkString
    } catch {
      case e: java.io.FileNotFoundException =>
        error(s"Error reading from file: ${e.getMessage}. Aborting workflow.")
        sys.exit(1)
    }
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[WorkflowConfig]("CreateWorkflow") {
      opt[String]("batch") action { (x, c) =>
        c.copy(batch = x)
      } text("Batch label of the workflow run.")
      opt[String]("engineFactory") required() action { (x, c) =>
        c.copy(engineFactory = x)
      } text("Class name of the engine's factory.")
      opt[String]("metricsClass") action { (x, c) =>
        c.copy(metricsClass = Some(x))
      } text("Class name of the run's metrics.")
      opt[String]("dsp") action { (x, c) =>
        c.copy(dataSourceParamsJsonPath = Some(x))
      } text("Path to data source parameters JSON file.")
      opt[String]("pp") action { (x, c) =>
        c.copy(preparatorParamsJsonPath = Some(x))
      } text("Path to preparator parameters JSON file.")
      opt[String]("ap") action { (x, c) =>
        c.copy(algorithmsParamsJsonPath = Some(x))
      } text("Path to algorithms parameters JSON file.")
      opt[String]("sp") action { (x, c) =>
        c.copy(servingParamsJsonPath = Some(x))
      } text("Path to serving parameters JSON file.")
      opt[String]("mp") action { (x, c) =>
        c.copy(metricsParamsJsonPath = Some(x))
      } text("Path to metrics parameters")
      opt[String]("jsonBasePath") action { (x, c) =>
        c.copy(jsonBasePath = x)
      } text("Base path to prepend to all parameters JSON files.")
      opt[String]("env") action { (x, c) =>
        c.copy(env = Some(x))
      } text("Comma-separated list of environmental variables (in 'FOO=BAR' " +
        "format) to pass to the Spark execution environment.")
    }

    parser.parse(args, WorkflowConfig()) map { wfc =>
      val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
      val engineModule = runtimeMirror.staticModule(wfc.engineFactory)
      val engineObject = runtimeMirror.reflectModule(engineModule)
      val engine = engineObject.instance.asInstanceOf[IEngineFactory]()
      val metrics = wfc.metricsClass.map { mc => //mc => null
        try {
          Class.forName(mc)
            .asInstanceOf[Class[Metrics[_ <: Params, _, _, _, _, _, _, _ <: AnyRef]]]
        } catch {
          case e: ClassNotFoundException =>
            error("Unable to obtain metrics class object ${mc}: " +
              s"${e.getMessage}. Aborting workflow.")
            sys.exit(1)
        }
      } //getOrElse(null)
      val dataSourceParams = wfc.dataSourceParamsJsonPath.map(p =>
        extractParams(
          stringFromFile(wfc.jsonBasePath, p),
          engine.dataSourceClass)).getOrElse(EmptyParams())
      val preparatorParams = wfc.preparatorParamsJsonPath.map(p =>
        extractParams(
          stringFromFile(wfc.jsonBasePath, p),
          engine.preparatorClass)).getOrElse(EmptyParams())
      val algorithmsParams: Seq[(String, Params)] =
        wfc.algorithmsParamsJsonPath.map { p =>
          val algorithmsParamsJson = parse(stringFromFile(wfc.jsonBasePath, p))
          algorithmsParamsJson match {
            case JArray(s) => s.map { algorithmParamsJValue =>
              val eap = algorithmParamsJValue.extract[AlgorithmParams]
              (
                eap.name,
                extractParams(
                  compact(render(eap.params)),
                  engine.algorithmClassMap(eap.name))
              )
            }
            case _ => Nil
          }
        } getOrElse Seq(("", EmptyParams()))
      val servingParams = wfc.servingParamsJsonPath.map(p =>
        extractParams(
          stringFromFile(wfc.jsonBasePath, p),
          engine.servingClass)).getOrElse(EmptyParams())
      val metricsParams = wfc.metricsParamsJsonPath.map(p =>
        //if (metrics == null)
        if (metrics.isEmpty)
          EmptyParams()
        else
          extractParams(
            stringFromFile(wfc.jsonBasePath, p),
            metrics.get)
      ) getOrElse EmptyParams()

      val engineParams = new EngineParams(
        dataSourceParams = dataSourceParams,
        preparatorParams = preparatorParams,
        algorithmParamsList = algorithmsParams,
        servingParams = servingParams)

      APIDebugWorkflow.runEngineTypeless(
        batch = wfc.batch,
        verbose = 3,
        engine = engine,
        engineParams = engineParams,
        null,
        null)
        //metricsClass = metrics.getOrElse(null),
        //metricsParams = metricsParams)
    }

    // dszeto: add these features next
    /*
    val pioEnvVars = arg.get.env.split(',').flatMap(p =>
      p.split('=') match {
        case Array(k, v) => List(k -> v)
        case _ => Nil
      }
    ).toMap
    val starttime = DateTime.now
    val evalWorkflow1 = EvaluationWorkflow.run(
      arg.get.batch,
      pioEnvVars,
      dataPrepParams,
      validatorParams,
      cleanserParams,
      algoParamSet,
      serverParams,
      engine,
      evaluator)
    val endtime = DateTime.now

    val runId = runs.insert(Run(
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

    info(s"Run ID: $runId")
    */

  }
}
