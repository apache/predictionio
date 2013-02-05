package io.prediction.output.itemrec

import io.prediction.commons.settings.{Algo, App, Engine}

import scala.util.Random

trait ItemRecAlgoOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, algo: Algo): Seq[String]
}

object ItemRecAlgoOutput {
  def output(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, engine: Engine, algo: Algo): Seq[String] = {
    /** Serendipity settings */
    val serendipity = engine.settings.get("serendipity") map { _.asInstanceOf[Int] }

    /** Serendipity value (s) from 0-10 in engine settings
      * Naively implemented as randomly picking items from top n*(s+1) results
      */
    val finalN = serendipity map { s => n*(s+1) } getOrElse n

    val output = algo.pkgname match {
      case "io.prediction.algorithms.scalding.itemrec.knnitembased" => knnitembased.ItemRecKNNItemBasedAlgoOutput.output(uid, finalN, itypes)
      case _ => throw new RuntimeException("Unsupported itemrec algorithm package: %s" format algo.pkgname)
    }

    /** Serendipity output */
    val finalOutput = serendipity map { s =>
      if (s > 0)
        Random.shuffle(output).take(n)
      else
        output
    } getOrElse output

    finalOutput
  }
}
