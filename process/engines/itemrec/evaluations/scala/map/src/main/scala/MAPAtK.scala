package io.prediction.metrics.itemrec.map

import io.prediction.commons.Config
import io.prediction.commons.settings.OfflineEvalResult
//import io.prediction.commons.appdata.{ Item, Items, U2IAction, U2IActions, User, Users }
import io.prediction.commons.filepath.OfflineMetricFile

//import java.io.{ BufferedWriter, File, FileWriter }
import scala.io.Source

//import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logger

case class MAPAtKConfig(
  appid: Int = 0,
  engineid: Int = 0,
  evalid: Int = 0,
  metricid: Int = 0,
  algoid: Int = 0,
  iteration: Int = 0,
  splitset: String = "",
  k: Int = 0,
  goal: String = "")

/**
 * Mean Average Precision at K for Single Machine
 *
 * TODO: Eliminate use of Config object. Let scheduler handles it all.
 */
object MAPAtK {
  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[MAPAtKConfig]("mapatk") {
      head("mapatk")
      opt[Int]("appid") required () action { (x, c) =>
        c.copy(appid = x)
      } text ("the App ID that this metric will be applied to")
      opt[Int]("engineid") required () action { (x, c) =>
        c.copy(engineid = x)
      } text ("the Engine ID that this metric will be applied to")
      opt[Int]("evalid") required () action { (x, c) =>
        c.copy(evalid = x)
      } text ("the OfflineEval ID that this metric will be applied to")
      opt[Int]("metricid") required () action { (x, c) =>
        c.copy(metricid = x)
      } text ("the OfflineEvalMetric ID that this metric will be applied to")
      opt[Int]("algoid") required () action { (x, c) =>
        c.copy(algoid = x)
      } text ("the Algo ID that this metric will be applied to")
      opt[Int]("iteration") required () action { (x, c) =>
        c.copy(iteration = x)
      } text ("the iteration number (starts from 1 for the 1st iteration and then increment for later iterations)")
      opt[String]("splitset") required () action { (x, c) =>
        c.copy(splitset = x)
      } validate { x =>
        if (x == "validation" || x == "test") success else failure("--splitset must be either 'validation' or 'test'")
      } text ("the split set (validation or test) that this metric will be run against")
      opt[Int]("k") required () action { (x, c) =>
        c.copy(k = x)
      } text ("the k parameter for MAP@k")
      opt[String]("goal") required () action { (x, c) =>
        c.copy(goal = x)
      } validate { x =>
        x match {
          case "view" | "conversion" | "like" | "rate3" | "rate4" | "rate5" => success
          case _ => failure("invalid goal specified")
        }
      } text ("actions to be treated as relevant (valid values: view, conversion, like, rate3, rate4, rate5)")
    }

    parser.parse(args, MAPAtKConfig()) map { config =>
      val logger = Logger(MAPAtK.getClass)
      val commonsConfig = new Config()
      val u2iDb = if (config.splitset == "validation") commonsConfig.getAppdataValidationU2IActions else commonsConfig.getAppdataTestU2IActions
      val resultsDb = commonsConfig.getSettingsOfflineEvalResults

      // Collect relevant items for all users
      logger.info("Collecting relevant items...")
      val relevantItems = scala.collection.mutable.Map[String, scala.collection.mutable.Set[String]]()
      u2iDb.getAllByAppid(config.evalid) filter { u2iAction =>
        config.goal match {
          case "view" | "conversion" | "like" => u2iAction.action == config.goal
          case "rate3" => try { u2iAction.action == "rate" && u2iAction.v.get.toInt >= 3 } catch {
            case e: Exception =>
              logger.error(s"${u2iAction.uid}-${u2iAction.iid}: ${u2iAction.v} (${e.getMessage()})")
              false
          }
          case "rate4" => try { u2iAction.action == "rate" && u2iAction.v.get.toInt >= 4 } catch {
            case e: Exception =>
              logger.error(s"${u2iAction.uid}-${u2iAction.iid}: ${u2iAction.v} (${e.getMessage()})")
              false
          }
          case "rate5" => try { u2iAction.action == "rate" && u2iAction.v.get.toInt == 5 } catch {
            case e: Exception =>
              logger.error(s"${u2iAction.uid}-${u2iAction.iid}: ${u2iAction.v} (${e.getMessage()})")
              false
          }
        }
      } foreach { u2iAction =>
        if (relevantItems.contains(u2iAction.uid)) {
          relevantItems(u2iAction.uid) += u2iAction.iid
        } else {
          relevantItems += (u2iAction.uid -> scala.collection.mutable.Set(u2iAction.iid))
        }
      }
      logger.info(s"# users: ${relevantItems.size}")

      // Read top-k list for every user
      val topKItems = scala.collection.mutable.Map[String, Seq[String]]()
      val topKFilePath = OfflineMetricFile(
        commonsConfig.settingsLocalTempRoot,
        config.appid,
        config.engineid,
        config.evalid,
        config.metricid,
        config.algoid,
        "topKItems.tsv")
      logger.info(s"Reading top-K list from: $topKFilePath")
      val prefixSize = config.evalid.toString.length + 1
      Source.fromFile(topKFilePath).getLines() foreach { topKLine =>
        val topKLineParts = topKLine.split("\t")
        topKItems += (topKLineParts(0).drop(prefixSize) -> topKLineParts(1).split(",").map(_.drop(prefixSize)))
      }

      val apAtK = topKItems map { t =>
        relevantItems.get(t._1) map { ri =>
          averagePrecisionAtK(config.k, t._2, ri.toSet)
        } getOrElse 0.0
      }
      val mapAtK = apAtK.sum / scala.math.min(topKItems.size, relevantItems.size)

      logger.info(s"MAP@${config.k} = $mapAtK. Saving results...")

      resultsDb.save(OfflineEvalResult(
        evalid = config.evalid,
        metricid = config.metricid,
        algoid = config.algoid,
        score = mapAtK,
        iteration = config.iteration,
        splitset = config.splitset))

      logger.info("MAP@${config.k} has run to completion")
    }
  }

  /**
   * Calculate the mean average precision @ k
   *
   * ap@k = sum(P(i)/min(m, k)) wher i=1 to k
   * k is number of prediction to be retrieved.
   * P(i) is the precision at position i of the top-K list
   *    if the item at position i is relevant, then P(i) = (the number of releavent items up to that position in the top-k list / position)
   *    if the item at position i is not relevant, then P(i)=0
   * m is the number of relevant items for this user.
   *
   * @return the average precision at k
   */
  def averagePrecisionAtK(k: Int, predictedItems: Seq[String], relevantItems: Set[String]): Double = {
    // supposedly the predictedItems.size should match k
    // NOTE: what if predictedItems is less than k? use the avaiable items as k.
    val n = scala.math.min(predictedItems.size, k)

    // find if each element in the predictedItems is one of the relevant items
    // if so, map to 1. else map to 0
    // (0, 1, 0, 1, 1, 0, 0)
    val relevantBinary: Seq[Int] = predictedItems.take(n).map { x => if (relevantItems(x)) 1 else 0 }
    val pAtKNom = relevantBinary.scanLeft(0)(_ + _).drop(1).zip(relevantBinary).map(t => if (t._2 != 0) t._1 else 0).map(_.toDouble)
    val pAtKDenom = 1 to relevantBinary.size
    val pAtK = pAtKNom zip pAtKDenom map { t => t._1 / t._2 }
    val apAtKDenom = scala.math.min(n, relevantItems.size)
    if (apAtKDenom == 0) 0 else pAtK.sum / apAtKDenom
  }
}
