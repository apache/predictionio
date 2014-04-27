package io.prediction.output.itemrank

import io.prediction.commons.Config
import io.prediction.commons.appdata.{ Item, Items }
import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{ Algo, App, Engine, OfflineEval }

import com.github.nscala_time.time.Imports._

trait ItemRankAlgoOutput {
  /** output the Seq of iids */
  def output(uid: String, iids: Seq[String], instant: DateTime)(implicit app: App, algo: Algo,
    offlineEval: Option[OfflineEval]): Seq[(String, Double)]
}

object ItemRankAlgoOutput {
  val config = new Config

  def freshnessOutput(output: Seq[(Item, Double)])(implicit app: App,
    engine: Engine, items: Items) = {
    val freshness = engine.params.get("freshness").map(_.asInstanceOf[Int])
    val freshnessTimeUnit = engine.params.get("freshnessTimeUnit").map(
      _.asInstanceOf[Long])
    /** Freshness output. */
    (freshness, freshnessTimeUnit) match {
      case (Some(f), Some(ftu)) => if (f > 0) {
        val recommendationTime = DateTime.now.millis
        output.map { itemAndScore =>
          val item = itemAndScore._1
          item.starttime map { st =>
            val timeDiff = (recommendationTime - st.millis) / 1000 / ftu
            if (timeDiff > 0)
              (itemAndScore._1, itemAndScore._2 * scala.math.exp(-timeDiff /
                (11 - f)))
            else
              itemAndScore
          } getOrElse itemAndScore
        }.sortBy(t => t._2).reverse
      } else output
      case _ => output
    }
  }

  /**
   * The ItemRec output does the following in sequence:
   *
   * - determine capabilities that needs to be handled by the engine
   * - compute the number of items for the engine to postprocess
   * - perform mandatory filtering (geo, time)
   * - perform postprocessing capabilities
   * - output items
   */
  def output(uid: String, iids: Seq[String])(implicit app: App, engine: Engine,
    algo: Algo, offlineEval: Option[OfflineEval] = None): Seq[String] = {
    val algoInfos = config.getSettingsAlgoInfos
    implicit val items = offlineEval map { _ =>
      config.getAppdataTrainingItems
    } getOrElse { config.getAppdataItems }

    /**
     * Determine capability of algo to see what this engine output layer needs
     * to handle.
     */
    val engineCapabilities = Seq("freshness")
    val algoCapabilities = algoInfos.get(algo.infoid).map(_.capabilities).
      getOrElse(Seq())
    val handledByEngine = engineCapabilities.filterNot(
      algoCapabilities.contains(_))

    // Time-dependent logic should reference to the same instant.
    val instant = DateTime.now

    /**
     * If no recommendations found for this user, simply return the original
     * list.
     */
    val ranked = ItemRankAllItemOutput.output(uid, iids, instant)
    val (rankedIids, rankedScores) = ranked.unzip
    val rankedIidsSet = rankedIids.toSet
    val rankedItemsMap = items.getByIds(app.id, rankedIids).map(x => x.id -> x)
      .toMap
    val rankedItems = rankedIids.map(x => rankedItemsMap(x))
    val unranked = iids.filterNot(x => rankedIidsSet(x))
    val preFinalOutput = handledByEngine
      .foldLeft(rankedItems.zip(rankedScores)) {
        (output, cap) =>
          cap match {
            case "freshness" => freshnessOutput(output)
          }
      }
    preFinalOutput.map(_._1.id) ++ unranked
  }
}
