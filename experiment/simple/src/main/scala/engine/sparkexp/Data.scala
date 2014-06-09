package io.prediction.engines.sparkexp

class EDP() extends BaseEvaluationDataParams with BaseValidationParams {}

class TDP(
  val s: String
) extends BaseTrainingDataParams {}

class VDP(
  val s: String
) extends BaseValidationDataParams {}

class TD(
  val d1: RDD[(String, String)],
  val d2: RDD[(String, String)]
) extends BasBaseTrainingData()

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
) extends BaseValidationResults {}

class CVR(
  val cvr: Double
) extends BaseCrossValidationResults{}
