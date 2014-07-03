package io.prediction


// FIXME(yipjustin). I am lazy...
import io.prediction.core._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

// One off evaluation. There is only one validation data set.
abstract class SimpleParallelDataPreparator[
    EDP <: BaseParams : Manifest, TD, F, A]
extends BaseDataPreparator[EDP, EmptyParams, EmptyParams, TD, F, A] {
  override def prepareBase(sc: SparkContext, params: BaseParams)
  : Map[Int, (EmptyParams, EmptyParams, TD, RDD[(F, A)])] = {
    val (td, rdd) = prepare(sc, params.asInstanceOf[EDP])

    Map(0 -> (EmptyParams(), EmptyParams(), td, rdd))
  }

  def prepare(sc: SparkContext, params: EDP): (TD, RDD[(F, A)])
}

// Pass all data to the final stage
abstract class SimpleValidator[VP <: BaseParams : Manifest, 
    F, P, A, CVR <: AnyRef]
extends BaseValidator[
    VP, EmptyParams,EmptyParams,
    F, P, A, (F, P, A), Seq[(F, P, A)], CVR] {
  def validateBase(input: (F, P, A)): (F, P, A) = input

  def validateSetBase(e0: EmptyParams, e1: EmptyParams, vus: Seq[(F, P, A)])
  : Seq[(F, P, A)] = vus

  def crossValidateBase(input: Seq[(EmptyParams, EmptyParams, Seq[(F, P, A)])])
  : CVR = {
    validate(input.map(_._3).flatten)
  }

  def validate(input: Seq[(F, P, A)]): CVR
}

