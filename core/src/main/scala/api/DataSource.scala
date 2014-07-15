package io.prediction.api

import io.prediction.core.BaseDataSource
import io.prediction.EmptyParams
import io.prediction.BaseParams
import org.apache.spark.rdd.RDD

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.reflect._

abstract class LDataSource[DSP <: BaseParams : ClassTag : Manifest,
    DP, TD : Manifest, Q, A]
  //extends BaseDataSource[DSP, EmptyParams, RDD[TD], Q, A] {
  extends BaseDataSource[DSP, DP, RDD[TD], Q, A] {

  def readBase(sc: SparkContext): Seq[(DP, RDD[TD], RDD[(Q, A)])] = {
    read.map { case (dp, td, qaSeq) => {
      (dp, sc.parallelize(Array(td)), sc.parallelize(qaSeq))
    }}
  }

  def read(): Seq[(DP, TD, Seq[(Q, A)])]
}

abstract class PDataSource[DSP <: BaseParams : ClassTag : Manifest, TD, Q, A]
  extends BaseDataSource[DSP, EmptyParams, TD, Q, A] {

  def readBase(sc: SparkContext): Seq[(EmptyParams, TD, RDD[(Q, A)])] = {
    read(sc).map { case (td, qaRdd) => {
      // TODO(yipjustin). Maybe do a size check on td, to make sure the user
      // doesn't supply a huge TD to the driver program.
      (EmptyParams(), td, qaRdd)
    }}
  }

  def read(sc: SparkContext): Seq[(TD, RDD[(Q, A)])]
}
