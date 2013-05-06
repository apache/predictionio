package io.prediction.evaluations.itemrec.paramgen

import io.prediction.commons._

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger

import scala.collection.immutable.Map

object ParamGen {
  def main(args: Array[String]) {
    val logger = Logger(ParamGen.getClass)

    val config = ConfigFactory.load

    val evalid = config.getInt("evalid")
    val algoid = config.getInt("algoid")
    val iteration = config.getInt("iteration")
    val n = config.getInt("n")

    val commonsConfig = new Config

    val algos = commonsConfig.getSettingsAlgos
    val algoinfos = commonsConfig.getSettingsAlgoInfos
    //val offlineEvals = commonsConfig.getSettingsOfflineEvals

    val algo = algos.get(algoid).get
    val algoinfo = algoinfos.get(algo.infoid).get
    //val offlineEval = offlineEvals.get(evalid).get

    /** Figure out what parameters can be tuned */
    val paramsToTune = algoinfo.paramdefaults.keySet filter { k =>
      algoinfo.paramdefaults.keySet.exists(e => s"${k}Min" == e) &&
        algoinfo.paramdefaults.keySet.exists(e => s"${k}Max" == e)
    }

    for (i <- 1 to n) {
      /** Pick a random value between intervals */
      val paramsValues = paramsToTune map { p =>
        val minValue = algoinfo.paramdefaults(s"${p}Min")
        val maxValue = algoinfo.paramdefaults(s"${p}Max")
        algoinfo.paramdefaults(s"${p}Min") match {
          case n: Int => p -> (minValue.asInstanceOf[Int] + scala.util.Random.nextInt(maxValue.asInstanceOf[Int] - minValue.asInstanceOf[Int]))
          case n: Double => p -> (minValue.asInstanceOf[Double] + scala.util.Random.nextDouble() * (maxValue.asInstanceOf[Double] - minValue.asInstanceOf[Double]))
        }
      }
      val algoToTune = algo.copy(
        offlineevalid = Some(evalid),
        iteration = Some(iteration),
        params = algo.params ++ paramsValues.toMap)
      println(algoToTune)
    }
  }
}
