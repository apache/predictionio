package io.prediction.core

import io.prediction.BaseParams
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class BaseDataSource[DSP <: BaseParams : ClassTag : Manifest,
    DP, TD, Q, A]
  extends AbstractDoer[DSP] {
  def readBase(sc: SparkContext): Seq[(DP, TD, RDD[(Q, A)])]
}

    

