package io.prediction.api

import io.prediction.core.BaseDataSource
import io.prediction.EmptyParams
import io.prediction.BaseParams
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class LDataSource[DSP <: BaseParams : ClassTag,
    TD : Manifest, F, A](dsp: DSP)
  extends BaseDataSource[DSP, EmptyParams, RDD[TD], F, A](dsp) {

  def readBase(sc: SparkContext): Seq[(EmptyParams, RDD[TD], RDD[(F, A)])] = {
    read.map { case (td, faSeq) => {
      (EmptyParams(), sc.parallelize(Array(td)), sc.parallelize(faSeq))
    }}
  }

  def read(): Seq[(TD, Seq[(F, A)])]
}

abstract class PDataSource[DSP <: BaseParams : Manifest, TD, F, A](dsp: DSP)
  extends BaseDataSource[DSP, EmptyParams, TD, F, A](dsp) {

  def readBase(sc: SparkContext): Seq[(EmptyParams, TD, RDD[(F, A)])] = {
    read(sc).map { case (td, faRdd) => {
      // TODO(yipjustin). Maybe do a size check on td, to make sure the user
      // doesn't supply a huge TD to the driver program.
      (EmptyParams(), td, faRdd)
    }}
  }

  def read(sc: SparkContext): Seq[(TD, RDD[(F, A)])]
}

