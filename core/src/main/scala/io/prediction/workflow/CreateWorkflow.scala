/** Copyright 2015 TappingStone, Inc.
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

import io.prediction.controller.Engine
import io.prediction.controller.Utils
import io.prediction.core.BaseEngine
import io.prediction.data.storage.EngineInstance
import io.prediction.data.storage.EvaluationInstance
import io.prediction.data.storage.Storage

import com.github.nscala_time.time.Imports._
import com.google.common.io.ByteStreams
import grizzled.slf4j.Logging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.write

import scala.language.existentials

object CreateWorkflow extends Logging {

  case class WorkflowConfig(
    deployMode: String = "",
    batch: String = "",
    engineId: String = "",
    engineVersion: String = "",
    engineVariant: String = "",
    engineFactory: String = "",
    engineParamsKey: String = "",
    evaluationClass: Option[String] = None,
    engineParamsGeneratorClass: Option[String] = None,
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
        if (basePath == "") {
          new Path(filePath)
        } else {
          new Path(basePath + Path.SEPARATOR + filePath)
        }
      new String(ByteStreams.toByteArray(fs.open(p)).map(_.toChar))
    } catch {
      case e: java.io.IOException =>
        error(s"Error reading from file: ${e.getMessage}. Aborting workflow.")
        sys.exit(1)
    }
  }

  val parser = new scopt.OptionParser[WorkflowConfig]("CreateWorkflow") {
    opt[String]("batch") action { (x, c) =>
      c.copy(batch = x)
    } text("Batch label of the workflow run.")
    opt[String]("engine-id") required() action { (x, c) =>
      c.copy(engineId = x)
    } text("Engine's ID.")
    opt[String]("engine-version") required() action { (x, c) =>
      c.copy(engineVersion = x)
    } text("Engine's version.")
    opt[String]("engine-variant") required() action { (x, c) =>
      c.copy(engineVariant = x)
    } text("Engine variant JSON.")
    opt[String]("evaluation-class") action { (x, c) =>
      c.copy(evaluationClass = Some(x))
    } text("Class name of the run's evaluator.")
    opt[String]("engine-params-generator-class") action { (x, c) =>
      c.copy(engineParamsGeneratorClass = Some(x))
    } text("Path to evaluator parameters")
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

  def main(args: Array[String]): Unit = {
    val wfcOpt = parser.parse(args, WorkflowConfig())
    if (wfcOpt.isEmpty) {
      logger.error("WorkflowConfig is empty. Quitting")
      return
    }

    val wfc = wfcOpt.get

    WorkflowUtils.modifyLogging(wfc.verbose)
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

    val engine: BaseEngine[_, _, _, _] = engineFactoryObj()

    val evaluation = wfc.evaluationClass.map { ec =>
      try {
        WorkflowUtils.getEvaluation(ec, getClass.getClassLoader)._2
      } catch {
        case e @ (_: ClassNotFoundException | _: NoSuchMethodException) =>
          error(s"Unable to obtain evaluation ${ec}. Aborting workflow.", e)
          sys.exit(1)
      }
    }

    val engineParamsGenerator = wfc.engineParamsGeneratorClass.map { epg =>
      try {
        WorkflowUtils.getEngineParamsGenerator(epg, getClass.getClassLoader)._2
      } catch {
        case e @ (_: ClassNotFoundException | _: NoSuchMethodException) =>
          error(s"Unable to obtain engine parameters generator ${epg}. " +
            "Aborting workflow.", e)
          sys.exit(1)
      }
    }

    val pioEnvVars = wfc.env.map(e =>
      e.split(',').flatMap(p =>
        p.split('=') match {
          case Array(k, v) => List(k -> v)
          case _ => Nil
        }
      ).toMap
    ).getOrElse(Map())

    val customSparkConf = WorkflowUtils.extractSparkConf(variantJson)
    val workflowParams = WorkflowParams(
      verbose = wfc.verbosity,
      skipSanityCheck = wfc.skipSanityCheck,
      stopAfterRead = wfc.stopAfterRead,
      stopAfterPrepare = wfc.stopAfterPrepare,
      sparkEnv = WorkflowParams().sparkEnv ++ customSparkConf)


    if (evaluation.isEmpty) {
      // Evaluator Not Specified. Do training.
      if (!engine.isInstanceOf[Engine[_,_,_,_,_,_]]) {
        throw new NoSuchMethodException(s"Engine $engine is not trainable")
      }

      val trainableEngine = engine.asInstanceOf[Engine[_, _, _, _, _, _]]

      val engineParams = if (wfc.engineParamsKey == "") {
        trainableEngine.jValueToEngineParams(variantJson)
      } else {
        engineFactoryObj.engineParams(wfc.engineParamsKey)
      }

      val engineInstance = EngineInstance(
        id = "",
        status = "INIT",
        startTime = DateTime.now,
        endTime = DateTime.now,
        engineId = wfc.engineId,
        engineVersion = wfc.engineVersion,
        engineVariant = variantId,
        engineFactory = engineFactory,
        evaluatorClass = wfc.evaluationClass.getOrElse(""),
        batch = (if (wfc.batch == "") engineFactory else wfc.batch),
        env = pioEnvVars,
        sparkConf = workflowParams.sparkEnv,
        dataSourceParams = write(engineParams.dataSourceParams),
        preparatorParams = write(engineParams.preparatorParams),
        algorithmsParams = write(engineParams.algorithmParamsList),
        servingParams = write(engineParams.servingParams),
        evaluatorParams = "",
        evaluatorResults = "",
        evaluatorResultsHTML = "",
        evaluatorResultsJSON = "")

      val engineInstanceId = Storage.getMetaDataEngineInstances.insert(
        engineInstance)

      CoreWorkflow.runTrain(
        env = pioEnvVars,
        params = workflowParams,
        engine = trainableEngine,
        engineParams = engineParams,
        engineInstance = engineInstance.copy(id = engineInstanceId))
    } else {
      val evaluationInstance = EvaluationInstance(
        evaluationClass = wfc.evaluationClass.get,
        engineParamsGeneratorClass = wfc.engineParamsGeneratorClass.get,
        batch = wfc.batch,
        env = pioEnvVars,
        sparkConf = workflowParams.sparkEnv
      )
      Workflow.runEvaluation(
        evaluation = evaluation.get,
        engineParamsGenerator = engineParamsGenerator.get,
        evaluationInstance = evaluationInstance,
        params = workflowParams)
    }
  }
}
