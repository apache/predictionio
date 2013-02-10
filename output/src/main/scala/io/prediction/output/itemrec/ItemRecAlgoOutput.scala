package io.prediction.output.itemrec

import io.prediction.commons.appdata.Config
import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{Algo, App, Engine}

import scala.util.Random

trait ItemRecAlgoOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]], after: Option[ItemRecScore])(implicit app: App, algo: Algo): Seq[ItemRecScore]
}

object ItemRecAlgoOutput {
  val appdataConfig = new Config
  val u2iActions = appdataConfig.getU2IActions

  def output(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, engine: Engine, algo: Algo): Seq[String] = {
    /** Serendipity settings. */
    val serendipity = engine.settings.get("serendipity") map { _.asInstanceOf[Int] }

    /** Serendipity value (s) from 0-10 in engine settings.
      * Implemented as randomly picking items from top n*(s+1) results.
      */
    val finalN = serendipity map { s => n*(s+1) } getOrElse n

    /** Filter unseen item.
      * Query U2I appdata with UID and IIDs.
      * If the result is non-zero, substract it from the current output.
      */
    var stopMore = false
    var after: Option[ItemRecScore] = None

    var outputBuffer = collection.mutable.ListBuffer[String]()

    while (outputBuffer.size < finalN && !stopMore) {
      val moreItemRecScores = more(uid, finalN, itypes, after)
      val moreIids = moreItemRecScores.map(_.iid).toList

      /** Stop the loop if no more scores can be found. */
      if (moreItemRecScores.size == 0)
        stopMore = true
      else {
        val seenItems = u2iActions.getAllByAppidAndUidAndIids(app.id, uid, moreIids)
        outputBuffer ++= (moreIids filterNot (seenItems.toList.map(_.iid) contains))
        after = Some(moreItemRecScores.last)
      }
    }

    val output = outputBuffer.toList

    /** Serendipity output. */
    val finalOutput = serendipity map { s =>
      if (s > 0)
        Random.shuffle(output).take(n)
      else
        output
    } getOrElse output


    finalOutput
  }

  /** Private method just to get items. */
  private def more(uid: String, n: Int, itypes: Option[List[String]], after: Option[ItemRecScore] = None)(implicit app: App, algo: Algo): Seq[ItemRecScore] = {
    algo.pkgname match {
      case "io.prediction.algorithms.scalding.itemrec.knnitembased" => knnitembased.ItemRecKNNItemBasedAlgoOutput.output(uid, n, itypes, after)
      case _ => throw new RuntimeException("Unsupported itemrec algorithm package: %s" format algo.pkgname)
    }
  }
}
