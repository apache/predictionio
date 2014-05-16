package io.prediction

trait BaseEvaluator[-EP, +TDP, +EDP, -F, -T] {
  def getParamsSet(params: EP): Seq[(TDP, EDP)]
  def evaluate(feature: F, predicted: T, actual: T): Unit
  def report(): Unit
}

trait BaseDataPreparator[-TDP, +TD] {
  def prepareTraining(params: TDP): TD
}

trait BaseEvaluationPreparator[-EDP, +F, +T] {
  def prepareEvaluation(params: EDP): Seq[(F, T)]
}

trait AbstractAlgorithm[-TD, -F, +T] {
  def initBase(baseAlgoParams: BaseAlgoParams): Unit
  def train(trainingData: TD): BaseModel
  def predictBaseModel(baseModel: BaseModel, feature: F): T
}

trait BaseAlgorithm[-TD, -F, +T, M <: BaseModel, AP <: BaseAlgoParams]
extends AbstractAlgorithm[TD, F, T] {
  override def initBase(baseAlgoParams: BaseAlgoParams) = 
    init(baseAlgoParams.asInstanceOf[AP])
  def init(algoParams: AP): Unit = {}

  override def train(trainingData: TD): BaseModel
  override def predictBaseModel(baseModel: BaseModel, feature: F): T = 
    predict(baseModel.asInstanceOf[M], feature)
  def predict(model: M, feature: F): T
}


trait BaseServer[-F, T] {
  def combine(feature: F, targets: Seq[T]): T
}

class DefaultServer[F, T] extends BaseServer[F, T] {
  override def combine(feature: F, targets: Seq[T]): T = targets.head
}

class BaseEngine[TDP, TD, F, T](
  val dataPreparatorClass: Class[_ <: BaseDataPreparator[TDP, TD]],
  val algorithmClass: Class[_ <: AbstractAlgorithm[TD, F, T]],
  val serverClass: Class[_ <: BaseServer[F, T]]) {
}


