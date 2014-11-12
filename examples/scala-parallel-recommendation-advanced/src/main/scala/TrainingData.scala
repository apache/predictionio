package org.examples.recommendation

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

class TrainingData(
  val ratings: RDD[Rating]
) extends Serializable {
  override def toString = {
    s"ratings: [${ratings.count()}] (${ratings.take(2).toList}...)"
  }
}
