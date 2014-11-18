package org.examples.recommendation

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

class PreparedData(
  val ratings: RDD[Rating]
) extends Serializable
