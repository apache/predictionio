package io.prediction.evaluations.itemrec.topkitems

import io.prediction.commons._
import io.prediction.output.itemrec.ItemRecAlgoOutput

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger
import org.rogach.scallop._

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val appidArg    = opt[Int]("appid", required = true)
  val engineidArg = opt[Int]("engineid", required = true)
  val evalidArg   = opt[Int]("evalid", required = true)
  val metricidArg = opt[Int]("metricid", required = true)
  val algoidArg   = opt[Int]("algoid", required = true)
}

object TopKItems {
  def main(args: Array[String]) {
    val logger = Logger(TopKItems.getClass)

    //val conf = new Conf(args)
    val config = ConfigFactory.load

    val evalid = config.getInt("evalid")
    val algoid = config.getInt("algoid")
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

    //users.getByAppid(conf.appidArg()) foreach { u =>
    users.getByAppid(evalid) foreach { u =>
      val topKItems = ItemRecAlgoOutput.output(u.id, k, None)(app, engine, algo, Some(offlineEval))
      if (topKItems.size > 0)
        logger.info(u.id+"\t"+topKItems.mkString(","))
    }
  }
}
