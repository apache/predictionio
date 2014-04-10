package io.prediction.algorithms.mahout.itemrec

import io.prediction.commons.modeldata.{ ItemRecScore }
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

object TestUtils {

  // NOTE: use HALF_UP mode to avoid error caused by rounding when compare data
  // (eg. 3.5 vs 3.499999999999).
  // (eg. 0.6666666666 vs 0.666666667)
  def roundUpScores(irec: ItemRecScore): ItemRecScore = {
    irec.copy(
      scores = irec.scores.map { x =>
        BigDecimal(x).setScale(9, BigDecimal.RoundingMode.HALF_UP).toDouble
      }
    )
  }

  def argMapToArray(args: Map[String, Any]): Array[String] = {
    args.toArray.flatMap {
      case (k, v) =>
        Array(s"--${k}", v.toString)
    }
  }

  def writeToFile(lines: List[String], filePath: String) = {
    val writer = new BufferedWriter(new FileWriter(new File(filePath)))
    lines.foreach { line =>
      writer.write(s"${line}\n")
    }
    writer.close()
  }

}
