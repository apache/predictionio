/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.workflow

import io.prediction.controller.EmptyParams
import io.prediction.controller.EngineParams
import io.prediction.controller.IEngineFactory
import io.prediction.controller.Evaluator
import io.prediction.controller.Params
import io.prediction.controller.Utils
import io.prediction.controller.WorkflowParams
import io.prediction.core.Doer
import io.prediction.core.BaseEvaluator
import io.prediction.data.storage.EngineInstance
import io.prediction.data.storage.Storage

import com.github.nscala_time.time.Imports._
import com.google.common.io.ByteStreams
import grizzled.slf4j.Logging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{ read, write }

import scala.language.existentials
import scala.reflect.Manifest
import scala.reflect.runtime.universe

import java.io.File

object CreateWorkflow extends Logging {

  case class WorkflowConfig(
    deployMode: String = "",
    batch: String = "",
    engineId: String = "",
    engineVersion: String = "",
    engineVariant: String = "",
    engineFactory: String = "",
    engineParamsKey: String = "",
    evaluatorClass: Option[String] = None,
    dataSourceParamsJsonPath: Option[String] = None,
    preparatorParamsJsonPath: Option[String] = None,
    algorithmsParamsJsonPath: Option[String] = None,
    servingParamsJsonPath: Option[String] = None,
    evaluatorParamsJsonPath: Option[String] = None,
    jsonBasePath: String = "",
    env: Option[String] = None,
    skipSanityCheck: Boolean = false,
    stopAfterRead: Boolean = false,
    stopAfterPrepare: Boolean = false,
    verbosity: Int = 0,
    verbose: Boolean = false,
    debug: Boolean = false,
    logFile: Option[String] = None)

  case class AlgorithmParams(name: String, params: JValue)

  implicit lazy val formats = Utils.json4sDefaultFormats +
    new NameParamsSerializer

  val hadoopConf = new Configuration
  val hdfs = FileSystem.get(hadoopConf)
  val localfs = FileSystem.getLocal(hadoopConf)

  private def stringFromFile(
      basePath: String,
      filePath: String,
      fs: FileSystem = hdfs): String = {
    try {
      val p =
        if (basePath == "")
          new Path(filePath)
        else
          new Path(basePath + Path.SEPARATOR + filePath)
      new String(ByteStreams.toByteArray(fs.open(p)).map(_.toChar))
    } catch {
      case e: java.io.IOException =>
        error(s"Error reading from file: ${e.getMessage}. Aborting workflow.")
        sys.exit(1)
    }
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[WorkflowConfig]("CreateWorkflow") {
      opt[String]("batch") action { (x, c) =>
        c.copy(batch = x)
      } text("Batch label of the workflow run.")
      opt[String]("engineId") required() action { (x, c) =>
        c.copy(engineId = x)
      } text("Engine's ID.")
      opt[String]("engineVersion") required() action { (x, c) =>
        c.copy(engineVersion = x)
      } text("Engine's version.")
      opt[String]("engineVariant") required() action { (x, c) =>
        c.copy(engineVariant = x)
      } text("Engine variant JSON.")
      opt[String]("evaluatorClass") action { (x, c) =>
        c.copy(evaluatorClass = Some(x))
      } text("Class name of the run's evaluator.")
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
        c.copy(evaluatorParamsJsonPath = Some(x))
      } text("Path to evaluator parameters")
      opt[String]("jsonBasePath") action { (x, c) =>
        c.copy(jsonBasePath = x)
      } text("Base path to prepend to all parameters JSON files.")
      opt[String]("env") action { (x, c) =>
        c.copy(env = Some(x))
      } text("Comma-separated list of environmental variables (in 'FOO=BAR' " +
        "format) to pass to the Spark execution environment.")
      opt[Unit]("verbose") action { (x, c) =>
        c.copy(verbose = true)
      } text("Enable verbose output.")
      opt[Unit]("debug") action { (x, c) =>
        c.copy(debug = true)
      } text("Enable debug output.")
      opt[Unit]("skip-sanity-check") action { (x, c) =>
        c.copy(skipSanityCheck = true)
      }
      opt[Unit]("stop-after-read") action { (x, c) =>
        c.copy(stopAfterRead = true)
      }
      opt[Unit]("stop-after-prepare") action { (x, c) =>
        c.copy(stopAfterPrepare = true)
      }
      opt[String]("deploy-mode") action { (x, c) =>
        c.copy(deployMode = x)
      }
      opt[Int]("verbosity") action { (x, c) =>
        c.copy(verbosity = x)
      }
      opt[String]("engine-factory") action { (x, c) =>
        c.copy(engineFactory = x)
      }
      opt[String]("engine-params-key") action { (x, c) =>
        c.copy(engineParamsKey = x)
      }
      opt[String]("log-file") action { (x, c) =>
        c.copy(logFile = Some(x))
      }
    }

    parser.parse(args, WorkflowConfig()) map { wfc =>
      WorkflowUtils.setupLogging(wfc.verbose, wfc.debug, "train", wfc.logFile)
      val targetfs = if (wfc.deployMode == "cluster") hdfs else localfs
      val variantJson = parse(stringFromFile("", wfc.engineVariant, targetfs))
      val engineFactory = if (wfc.engineFactory == "") {
        variantJson \ "engineFactory" match {
          case JString(s) => s
          case _ =>
            error("Unable to read engine factory class name from " +
              s"${wfc.engineVariant}. Aborting.")
              sys.exit(1)
        }
      } else wfc.engineFactory
      val variantId = variantJson \ "id" match {
        case JString(s) => s
        case _ =>
          error("Unable to read engine variant ID from " +
            s"${wfc.engineVariant}. Aborting.")
          sys.exit(1)
      }
      val (engineLanguage, engineFactoryObj) = try {
        WorkflowUtils.getEngine(engineFactory, getClass.getClassLoader)
      } catch {
        case e @ (_: ClassNotFoundException | _: NoSuchMethodException) =>
          error(s"Unable to obtain engine: ${e.getMessage}. Aborting workflow.")
          sys.exit(1)
      }
      val engine = engineFactoryObj()
      val evaluator = wfc.evaluatorClass.map { mc => //mc => null
        try {
          Class.forName(mc)
            .asInstanceOf[Class[BaseEvaluator[_, _, _, _, _, _, _ <: AnyRef]]]
        } catch {
          case e: ClassNotFoundException =>
            error("Unable to obtain evaluator class object ${mc}: " +
              s"${e.getMessage}. Aborting workflow.")
            sys.exit(1)
        }
      }

      val engineParams = if (wfc.engineParamsKey == "") {
        info(s"Extracting datasource params...")
        val dataSourceParams: (String, Params) =
          WorkflowUtils.getParamsFromJsonByFieldAndClass(
            variantJson,
            "datasource",
            engine.dataSourceClassMap,
            engineLanguage)
        info(s"datasource: ${dataSourceParams}")

        info(s"Extracting preparator params...")
        val preparatorParams: (String, Params) =
          WorkflowUtils.getParamsFromJsonByFieldAndClass(
            variantJson,
            "preparator",
            engine.preparatorClassMap,
            engineLanguage)
        info(s"preparator: ${preparatorParams}")

        val algorithmsParams: Seq[(String, Params)] =
          variantJson findField {
            case JField("algorithms", _) => true
            case _ => false
          } map { jv =>
            val algorithmsParamsJson = jv._2
            algorithmsParamsJson match {
              case JArray(s) => s.map { algorithmParamsJValue =>
                val eap = algorithmParamsJValue.extract[AlgorithmParams]
                (
                  eap.name,
                  WorkflowUtils.extractParams(
                    engineLanguage,
                    compact(render(eap.params)),
                    engine.algorithmClassMap(eap.name))
                )
              }
              case _ => Nil
            }
          } getOrElse Seq(("", EmptyParams()))

        info(s"Extracting serving params...")
        val servingParams: (String, Params) =
          WorkflowUtils.getParamsFromJsonByFieldAndClass(
            variantJson,
            "serving",
            engine.servingClassMap,
            engineLanguage)
        info(s"serving: ${servingParams}")

        new EngineParams(
          dataSourceParams = dataSourceParams,
          preparatorParams = preparatorParams,
          algorithmParamsList = algorithmsParams,
          servingParams = servingParams)
      } else {
        engineFactoryObj.engineParams(wfc.engineParamsKey)
      }

      val evaluatorParams = wfc.evaluatorParamsJsonPath.map(p =>
        if (evaluator.isEmpty)
          EmptyParams()
        else
          WorkflowUtils.extractParams(
            engineLanguage,
            stringFromFile(wfc.jsonBasePath, p),
            evaluator.get)
      ) getOrElse EmptyParams()

      val evaluatorInstance = evaluator
        .map(m => Doer(m, evaluatorParams))
        .getOrElse(null)

      val pioEnvVars = wfc.env.map(e =>
        e.split(',').flatMap(p =>
          p.split('=') match {
            case Array(k, v) => List(k -> v)
            case _ => Nil
          }
        ).toMap
      ).getOrElse(Map())

      val engineInstance = EngineInstance(
        id = "",
        status = "INIT",
        startTime = DateTime.now,
        endTime = DateTime.now,
        engineId = wfc.engineId,
        engineVersion = wfc.engineVersion,
        engineVariant = variantId,
        engineFactory = engineFactory,
        evaluatorClass = wfc.evaluatorClass.getOrElse(""),
        batch = (if (wfc.batch == "") engineFactory else wfc.batch),
        env = pioEnvVars,
        dataSourceParams = write(engineParams.dataSourceParams),
        preparatorParams = write(engineParams.preparatorParams),
        algorithmsParams = write(engineParams.algorithmParamsList),
        servingParams = write(engineParams.servingParams),
        evaluatorParams = write(evaluatorParams),
        evaluatorResults = "",
        evaluatorResultsHTML = "",
        evaluatorResultsJSON = "")
      val engineInstanceId = Storage.getMetaDataEngineInstances.insert(
        engineInstance)

      CoreWorkflow.runEngineTypeless(
        env = pioEnvVars,
        params = WorkflowParams(
          verbose = wfc.verbosity,
          batch = (if (wfc.batch == "") engineFactory else wfc.batch),
          skipSanityCheck = wfc.skipSanityCheck,
          stopAfterRead = wfc.stopAfterRead,
          stopAfterPrepare = wfc.stopAfterPrepare),
        engine = engine,
        engineParams = engineParams,
        evaluator = evaluatorInstance,
        evaluatorParams = evaluatorParams,
        engineInstance = Some(engineInstance.copy(id = engineInstanceId)))
    }
  }
}
