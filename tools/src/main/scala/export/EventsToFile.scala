package io.prediction.tools.export

import io.prediction.controller.Utils
import io.prediction.data.storage.EventJson4sSupport
import io.prediction.data.storage.Storage
import io.prediction.tools.Runner
import io.prediction.workflow.WorkflowContext
import io.prediction.workflow.WorkflowUtils

import grizzled.slf4j.Logging
import org.apache.spark.sql.SQLContext
import org.json4s.native.Serialization._

case class EventsToFileArgs(
  env: String = "",
  logFile: String = "",
  appId: Int = 0,
  outputPath: String = "",
  format: String = "parquet",
  verbose: Boolean = false,
  debug: Boolean = false)

object EventsToFile extends Logging {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[EventsToFileArgs]("EventsToFile") {
      opt[String]("env") action { (x, c) =>
        c.copy(env = x)
      }
      opt[String]("log-file") action { (x, c) =>
        c.copy(logFile = x)
      }
      opt[Int]("appid") action { (x, c) =>
        c.copy(appId = x)
      }
      opt[String]("format") action { (x, c) =>
        c.copy(format = x)
      }
      opt[String]("output") action { (x, c) =>
        c.copy(outputPath = x)
      }
      opt[Unit]("verbose") action { (x, c) =>
        c.copy(verbose = true)
      }
      opt[Unit]("debug") action { (x, c) =>
        c.copy(debug = true)
      }
    }
    parser.parse(args, EventsToFileArgs()) map { args =>
      WorkflowUtils.setupLogging(verbose = args.verbose, debug = args.debug)
      @transient lazy implicit val formats = Utils.json4sDefaultFormats +
        new EventJson4sSupport.APISerializer
      val sc = WorkflowContext(
        mode = "Export",
        batch = "App ID " + args.appId,
        executorEnv = Runner.envStringToMap(args.env))
      val sqlContext = new SQLContext(sc)
      val events = Storage.getPEvents()
      val eventsRdd = events.find(appId = args.appId)(sc)
      val jsonStringRdd = eventsRdd.map(write(_))
      if (args.format == "json") {
        jsonStringRdd.saveAsTextFile(args.outputPath)
      } else {
        val jsonRdd = sqlContext.jsonRDD(jsonStringRdd)
        info(jsonRdd.schemaString)
        jsonRdd.saveAsParquetFile(args.outputPath)
      }
    }
  }
}
