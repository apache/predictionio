package io.prediction.output.itemsim

import io.prediction.commons.Config
import io.prediction.commons.modeldata.ItemSimScore
import io.prediction.commons.settings.{Algo, App, Engine, OfflineEval}

import scala.util.Random

trait ItemSimAlgoOutput {
  def output(iid: String, n: Int, itypes: Option[Seq[String]], after: Option[ItemSimScore])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]): Seq[ItemSimScore]
}

object ItemSimAlgoOutput {
  val config = new Config
  val items = config.getAppdataItems

  def output(iid: String, n: Int, itypes: Option[Seq[String]])(implicit app: App, engine: Engine, algo: Algo, offlineEval: Option[OfflineEval] = None): Seq[String] = {
    /** Serendipity settings. */
    val serendipity = engine.settings.get("serendipity") map { _.asInstanceOf[Int] }

    /** Serendipity value (s) from 0-10 in engine settings.
      * Implemented as randomly picking items from top n*(s+1) results.
      */
    val finalN = serendipity map { s => n*(s+1) } getOrElse n

    /** At this point "output" is guaranteed to have n*(s+1) items (seen or unseen) unless model data is exhausted. */
    val output = more(iid, finalN, itypes, None) map { _.simiid }

    /** Serendipity output. */
    val serendipityOutput = serendipity map { s =>
      if (s > 0)
        Random.shuffle(output).take(n)
      else
        output
    } getOrElse output

    /** Freshness (0 <= f <= 10) is implemented as the ratio of final results being top N results re-sorted by start time.
      * E.g. For f = 4, 40% of the final output will consist of top N results re-sorted by start time.
      */
    val freshness = engine.settings.get("freshness") map { _.asInstanceOf[Int] }

    /** Freshness output. */
    val finalOutput = freshness map { f =>
      if (f > 0) {
        val freshnessN = scala.math.round(n*f/10)
        val otherN = n-freshnessN
        val freshnessOutput = items.getRecentByIds(app.id, output).map(_.id)
        freshnessOutput.take(freshnessN) ++ serendipityOutput.take(otherN)
      } else
        serendipityOutput
    } getOrElse serendipityOutput

    finalOutput
  }

  /** Private method just to get items. */
  private def more(iid: String, n: Int, itypes: Option[Seq[String]], after: Option[ItemSimScore] = None)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]): Seq[ItemSimScore] = {
    /** To be refactored with real time algorithms.
      * Temporarily enable any batch algorithms.  */
    ItemSimCFAlgoOutput.output(iid, n, itypes, after)
  }
}
