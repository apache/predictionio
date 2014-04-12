package io.prediction.algorithms.mahout.itemrec

import scala.io.Source

object MahoutCommons {
  case class ItemData(
    val iid: String,
    val itypes: Seq[String],
    val starttime: Long)

  // item index file (iindex iid itypes starttime)
  // iindex -> ItemData
  def itemsMap(input: String): Map[Long, ItemData] = Source.fromFile(input)
    .getLines().map[(Long, ItemData)] { line =>
      val (iindex, item) = try {
        val fields = line.split("\t")
        val itemData = ItemData(
          iid = fields(1),
          itypes = fields(2).split(",").toSeq,
          starttime = fields(3).toLong)
        (fields(0).toLong, itemData)
      } catch {
        case e: Exception =>
          throw new RuntimeException(
            s"Cannot get item info in line: ${line}. ${e}")
      }
      (iindex, item)
    }.toMap
}
