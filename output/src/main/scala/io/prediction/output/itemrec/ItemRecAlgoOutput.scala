package io.prediction.output.itemrec

import io.prediction.commons.Config
import io.prediction.commons.appdata.{ Item, Items }
import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{ Algo, App, Engine, OfflineEval }

import scala.util.Random
import scala.collection.mutable

import com.github.nscala_time.time.Imports._

trait ItemRecAlgoOutput {
  /** output the Seq of iids */
  def output(uid: String, n: Int, itypes: Option[Seq[String]])(
    implicit app: App, algo: Algo,
    offlineEval: Option[OfflineEval]): Seq[(String, Double)]
}

object ItemRecAlgoOutput {
  val config = new Config

  def serendipityN(n: Int)(implicit engine: Engine) = {
    /** Serendipity settings. */
    val serendipity = engine.params.get("serendipity").map(_.asInstanceOf[Int])

    /**
     * Serendipity value (s) from 0-10 in engine settings.
     * Implemented as randomly picking items from top n*(s+1) results.
     */
    serendipity.map { s => n * (s + 1) }.getOrElse(n)
  }

  def serendipityOutput(output: Seq[(Item, Double)],
    n: Int)(implicit engine: Engine) = {
    val serendipity = engine.params.get("serendipity").map(_.asInstanceOf[Int])
    /** Serendipity output. */
    serendipity.map { s =>
      if (s > 0)
        Random.shuffle(output.take(n * (s + 1)))
      else
        output
    } getOrElse output
  }

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

  private def dedupByAttribute(output: Seq[(Item, Double)],
    attribute: String) = {
    val seenValueSet = mutable.Set[String]()
    output.filter { itemAndScore =>
      if (itemAndScore._1.attributes.isEmpty) {
        false
      } else if (!itemAndScore._1.attributes.get.contains(attribute)) {
        // If item doesn't have the attribute, drop the item
        false
      } else {
        val attributeValue = itemAndScore._1.attributes.get(attribute)
          .asInstanceOf[String]
        if (seenValueSet(attributeValue)) {
          false
        } else {
          seenValueSet.add(attributeValue)
          true
        }
      }
    }
  }

  def dedupOutput(output: Seq[(Item, Double)])(implicit engine: Engine) = {
    val attribute = engine.params.get("dedupByAttribute")
      .map(_.asInstanceOf[String])

    if (!attribute.isEmpty && attribute.get != "")
      dedupByAttribute(output, attribute.get)
    else
      output
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
  def output(uid: String, n: Int, itypes: Option[Seq[String]],
    latlng: Option[Tuple2[Double, Double]], within: Option[Double],
    unit: Option[String])(implicit app: App, engine: Engine, algo: Algo,
      offlineEval: Option[OfflineEval] = None): Seq[String] = {
    val algoInfos = config.getSettingsAlgoInfos
    implicit val items = offlineEval map { _ =>
      config.getAppdataTrainingItems
    } getOrElse { config.getAppdataItems }

    /**
     * Determine capability of algo to see what this engine output layer needs
     * to handle.
     */
    val engineCapabilities = Seq("dedupByAttribute", "freshness", "serendipity")
    val algoCapabilities = algoInfos.get(algo.infoid).map(_.capabilities).
      getOrElse(Seq())
    val handledByEngine = engineCapabilities.filterNot(
      algoCapabilities.contains(_))

    // Determine the number of items to process in the filtering stage.
    val filterN = handledByEngine.foldLeft(n) { (n, cap) =>
      if (cap == "serendipity") serendipityN(n) else n
    }

    /**
     * At the moment, PredictionIO depends only on MongoDB for its model data
     * storage. Since we are still using the legacy longitude-latitude format,
     * the maximum number of documents that can be returned from a query with
     * geospatial constraint is 100. A "manual join" is still feasible with this
     * size.
     */
    val iidsAndScores =
      latlng.map { ll =>
        val geoItems = items.getByAppidAndLatlng(app.id, ll, within, unit)
          .map(_.id).toSet
        // use n = 0 to return all available iids for now
        ItemRecCFAlgoOutput.output(uid, 0, itypes).filter(t => geoItems(t._1))
      }.getOrElse {
        // use n = 0 to return all available iids for now
        ItemRecCFAlgoOutput.output(uid, 0, itypes)
      }.toSeq
    val iids = iidsAndScores map { _._1 }

    /** Start and end time filtering. */
    val itemsForTimeCheck = items.getByIds(app.id, iids)
    val iidsWithValidTimeMap = (itemsForTimeCheck filter { item =>
      (item.starttime, item.endtime) match {
        case (Some(st), None) => DateTime.now >= st
        case (None, Some(et)) => DateTime.now <= et
        case (Some(st), Some(et)) => st <= DateTime.now && DateTime.now <= et
        case _ => true
      }
    }).map(item => (item.id, item)).toMap

    val itemsAndScoresWithValidTime = iidsAndScores
      .filter(t => iidsWithValidTimeMap.contains(t._1))
      .map(t => (iidsWithValidTimeMap(t._1), t._2))

    val finalOutput = handledByEngine.foldLeft(itemsAndScoresWithValidTime) {
      (output, cap) =>
        cap match {
          case "dedupByAttribute" => dedupOutput(output)
          case "freshness" => freshnessOutput(output)
          case "serendipity" => serendipityOutput(output, n)
        }
    }

    finalOutput.take(n).map(_._1.id)
  }
}
