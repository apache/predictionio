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

  def prepare(sc: SparkContext, edp: EDP): (TD, RDD[(F, A)])
}

abstract class SimpleLocalDataPreparator[
    EDP <: BaseParams : Manifest, TD : Manifest, F, A]
extends BaseDataPreparator[EDP, EmptyParams, EmptyParams, RDD[TD], F, A] {
  override def prepareBase(sc: SparkContext, params: BaseParams)
  : Map[Int, (EmptyParams, EmptyParams, RDD[TD], RDD[(F, A)])] = {
    val (td, faSeq) = prepare(params.asInstanceOf[EDP])
    val faRdd = sc.parallelize(faSeq.toSeq)
    val tdRdd = sc.parallelize(Array(td))
    Map(0 -> (EmptyParams(), EmptyParams(), tdRdd, faRdd))
  }

  def prepare(edp: EDP): (TD, Iterable[(F, A)])
}



/*
abstract class SimpleLocalDataPreparator[
    EDP <: BaseParams : Manifest, TD, F, A]
extends BaseDataPreparator[EDP, EmptyParams, EmptyParams, TD, F, A] {
  override def prepareBase(sc: SparkContext, params: BaseParams)
  : Map[Int, (EmptyParams, EmptyParams, TD, RDD[(F, A)])] = {
    val (td, faSeq) = prepare(params.asInstanceOf[EDP])

    val rdd = sc.parallelize(faSeq.toSeq)

    Map(0 -> (EmptyParams(), EmptyParams(), td, rdd))
  }

  def prepare(params: EDP): (TD, Iterable[(F, A)])
}
*/



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



// When Prediction and Actual are both double
class MeanSquareErrorValidator[F]
  extends SimpleValidator[EmptyParams, F, Double, Double, String] {
  def validate(input: Seq[(F, Double, Double)]): String = {
    val units = input.map(e => math.pow((e._2 - e._3), 2))
    val mse = units.sum / units.length
    f"MSE: ${mse}%8.6f"
  }
}
