package io.prediction.core

import io.prediction.BaseParams
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

abstract class BaseDataSource[DSP <: BaseParams : Manifest, DUP <: BaseParams,
    TD, F, A](dsp: DSP)
  extends AbstractDoer[DSP](dsp) {

  def prepareBase(sc: SparkContext): Seq[(DUP, TD, RDD[(F, A)])]
}

    

