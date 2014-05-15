package io.prediction

import io.prediction.storage.Config
import io.prediction.storage.{ ItemTrend, ItemTrends }

object First {
  val config = new Config

  def main(args: Array[String]) {
    println("Above us only sky")


    val itemTrendsDb = config.getAppdataItemTrends

    val itemTrends = itemTrendsDb.get(9527, "GOOG")

    println(itemTrends)

  }
}
