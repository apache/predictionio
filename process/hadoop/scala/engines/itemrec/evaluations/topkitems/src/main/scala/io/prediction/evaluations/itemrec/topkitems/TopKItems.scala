package io.prediction.evaluations.itemrec.topkitems

import io.prediction.commons._
import io.prediction.commons.filepath.OfflineMetricFile
import io.prediction.output.itemrec.ItemRecAlgoOutput

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger
import java.io.File
import scala.sys.process._
import scalax.io._

object TopKItems {
  def main(args: Array[String]) {
    val logger = Logger(TopKItems.getClass)

    val config = ConfigFactory.load

    val evalid = config.getInt("evalid")
    val algoid = config.getInt("algoid")
    val metricid = config.getInt("metricid")
    val hdfsRoot = config.getString("hdfsroot")
    val k = config.getInt("k")

    val trainingSetConfig = new appdata.TrainingSetConfig
    val settingsConfig = new settings.Config

    val apps = settingsConfig.getApps
    val engines = settingsConfig.getEngines
    val algos = settingsConfig.getAlgos
    val offlineEvals = settingsConfig.getOfflineEvals
    val users = trainingSetConfig.getUsers

    val algo = algos.get(algoid).get
    val offlineEval = offlineEvals.get(evalid).get
    val engine = engines.get(offlineEval.engineid).get
    val app = apps.get(engine.appid).get.copy(id = evalid)

    val tmpFile = File.createTempFile("pdio-", ".topk")
    tmpFile.deleteOnExit
    val output: Output = Resource.fromFile(tmpFile)
    logger.info("Dumping data to temporary file %s...".format(tmpFile))

    var userCount = 0
    users.getByAppid(evalid) foreach { u =>
      val topKItems = ItemRecAlgoOutput.output(u.id, k, None)(app, engine, algo, Some(offlineEval))
      if (topKItems.length > 0) {
        userCount += 1
        output.write("%d_%s\t%s\n".format(evalid, u.id, topKItems.map(iid => "%d_%s".format(evalid, iid)).mkString(",")))
      }
    }
    logger.info("Found %d user(s) with non-zero top-K items".format(userCount))

    val hdfsFile = OfflineMetricFile(hdfsRoot, engine.appid, engine.id, evalid, metricid, algoid, "topKItems.tsv")

    val rmCommand = "hadoop fs -rm %s".format(hdfsFile)
    logger.info("Executing '%s'...".format(rmCommand))
    rmCommand.!

    val copyCommand = "hadoop fs -copyFromLocal %s %s".format(tmpFile, hdfsFile)
    logger.info("Executing '%s'...".format(copyCommand))
    copyCommand.!

    logger.info("Finished")
  }
}
