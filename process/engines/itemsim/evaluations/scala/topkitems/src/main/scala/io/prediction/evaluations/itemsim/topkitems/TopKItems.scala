package io.prediction.evaluations.itemsim.topkitems

import io.prediction.commons._
import io.prediction.commons.filepath.OfflineMetricFile
import io.prediction.output.itemsim.ItemSimAlgoOutput

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

    val commonsConfig = new Config

    /** Try search path if hadoop home is not set. */
    val hadoopCommand = commonsConfig.settingsHadoopHome map { h => h+"/bin/hadoop" } getOrElse { "hadoop" }

    val apps = commonsConfig.getSettingsApps
    val engines = commonsConfig.getSettingsEngines
    val algos = commonsConfig.getSettingsAlgos
    val offlineEvals = commonsConfig.getSettingsOfflineEvals
    val items = commonsConfig.getAppdataTrainingItems

    val algo = algos.get(algoid).get
    val offlineEval = offlineEvals.get(evalid).get
    val engine = engines.get(offlineEval.engineid).get
    val app = apps.get(engine.appid).get.copy(id = evalid)

    val tmpFile = File.createTempFile("pdio-", ".topk", new File(commonsConfig.settingsLocalTempRoot))
    tmpFile.deleteOnExit
    val output: Output = Resource.fromFile(tmpFile)
    logger.info(s"Dumping data to temporary file ${tmpFile}...")

    val scores = Seq.range(1, k + 1).reverse

    var itemCount = 0
    items.getByAppid(evalid) foreach { i =>
      val topKItems = ItemSimAlgoOutput.output(i.id, k, None)(app, engine, algo, Some(offlineEval))
      if (topKItems.length > 0) {
        itemCount += 1
        topKItems.zip(scores) foreach { tuple =>
          val (iid, score) = tuple
          output.write(s"${evalid}_${i.id}\t${evalid}_${iid}\t${score}\n")
        }
      }
    }
    logger.info(s"Found ${itemCount} item(s) with non-zero top-K items")

    val hdfsFile = OfflineMetricFile(hdfsRoot, engine.appid, engine.id, evalid, metricid, algoid, "topKItems.tsv")

    val rmCommand = s"$hadoopCommand fs -rm $hdfsFile"
    logger.info(s"Executing '${rmCommand}'...")
    rmCommand.!

    val copyCommand = s"$hadoopCommand fs -copyFromLocal $tmpFile $hdfsFile"
    logger.info(s"Executing '${copyCommand}'...")
    copyCommand.!

    logger.info("Finished")
  }
}
