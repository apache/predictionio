package io.prediction.evaluations.itemrec.paramgen

import io.prediction.commons._

import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logger

import scala.collection.immutable.Map

object ParamGen {
  def main(args: Array[String]) {
    val logger = Logger(ParamGen.getClass)

    val config = ConfigFactory.load

    val evalids = config.getString("evalids")
    val algoid = config.getInt("algoid")
    val loop = config.getInt("loop")
    val paramsets = config.getInt("paramsets")

    val commonsConfig = new Config

    val algos = commonsConfig.getSettingsAlgos

    val algo = algos.get(algoid).get

    /** Figure out what parameters can be tuned */
    val paramsToTune = algo.params.keySet filter { k =>
      algo.params.keySet.exists(e => s"${k}Min" == e) &&
        algo.params.keySet.exists(e => s"${k}Max" == e)
    }

    for (i <- 1 to paramsets) {
      /** Pick a random value between intervals */
      val paramsValues = paramsToTune map { p =>
        val minValue = algo.params(s"${p}Min")
        val maxValue = algo.params(s"${p}Max")
        algo.params(s"${p}Min") match {
          case n: Int => p -> (minValue.asInstanceOf[Int] + scala.util.Random.nextInt(maxValue.asInstanceOf[Int] - minValue.asInstanceOf[Int]))
          case n: Double => p -> (minValue.asInstanceOf[Double] + scala.util.Random.nextDouble() * (maxValue.asInstanceOf[Double] - minValue.asInstanceOf[Double]))
        }
      }
      evalids.split(",") foreach { evalid =>
        val algoToTune = algo.copy(
          offlineevalid = Some(evalid.toInt),
          loop = Some(loop),
          params = algo.params ++ paramsValues.toMap,
          paramset = Some(i),
          status = "simeval")
        algos.insert(algoToTune)
      }
    }
  }
}
