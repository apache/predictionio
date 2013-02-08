package io.prediction.output.itemrec

import io.prediction.commons.appdata.Config
import io.prediction.commons.settings.{Algo, App, Engine}

import scala.util.Random

trait ItemRecAlgoOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, algo: Algo): Seq[String]
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

    val output = algo.pkgname match {
      case "io.prediction.algorithms.scalding.itemrec.knnitembased" => knnitembased.ItemRecKNNItemBasedAlgoOutput.output(uid, finalN, itypes)
      case _ => throw new RuntimeException("Unsupported itemrec algorithm package: %s" format algo.pkgname)
    }

    /** Serendipity output. */
    val finalOutput = serendipity map { s =>
      if (s > 0)
        Random.shuffle(output).take(n)
      else
        output
    } getOrElse output

    /** Filter unseen item.
      * Query U2I appdata with UID and IIDs.
      * If the result is non-zero, substract it from the current output.
      */
    val seenItems = u2iActions.getAllByAppidAndUidAndIids(app.id, uid, finalOutput)
    val unseenItems = finalOutput -- seenItems.toList.map(_.iid)

    unseenItems
  }
}
