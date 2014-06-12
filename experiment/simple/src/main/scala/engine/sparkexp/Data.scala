package io.prediction.engines.sparkexp

import io.prediction.{
  //BaseEvaluationParams,
  BaseValidationParams,
  BaseEvaluationDataParams,
  BaseTrainingDataParams,
  BaseValidationDataParams,
  BaseTrainingData,
  BaseCleansedData,
  BaseFeature,
  BasePrediction,
  BaseActual,
  BaseModel,
  BaseAlgoParams,
  BaseValidationUnit,
  BaseValidationResults,
  BaseCrossValidationResults
}

import org.apache.spark.rdd.RDD

class EDP() extends BaseEvaluationDataParams with BaseValidationParams {}

class TDP(
  val s: String
) extends BaseTrainingDataParams {
  override def toString = s"${s}"
}

class VDP(
  val s: String
) extends BaseValidationDataParams {
  override def toString = s"${s}"
}

class TD(
  val d1: RDD[(String, String)],
  val d2: RDD[(String, String)]
) extends BaseTrainingData()

/*
class CD(
  val d1: RDD[(String, String)],
  val d2: RDD[(String, String)]
) extends BaseCleansedData()
*/

class AP() extends BaseAlgoParams {}

class M(
  val m1: RDD[(String, String)],
  val m2: RDD[(String, String)]
) extends BaseModel {}

class F(
  val f: String
) extends BaseFeature {}

class P(
  val p: String
) extends BasePrediction{}

class A(
  val a: String
) extends BaseActual {}

class VU(
  val f: String,
  val p: String,
  val a: String,
  val vu: Int
) extends BaseValidationUnit {}

class VR(
  val vr: Int
) extends BaseValidationResults {
  override def toString = s"${vr}"
}

class CVR(
  val cvr: Double
) extends BaseCrossValidationResults{}
