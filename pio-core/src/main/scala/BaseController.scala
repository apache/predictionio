package io.prediction

trait BaseEvaluator[-EP, +TDP, +EDP, -F, -T] {
  def getParamsSet(params: EP): Seq[(TDP, EDP)]
  def evaluate(feature: F, predicted: T, actual: T): Unit
  def report(): Unit
}

trait BaseDataPreparator[-TDP, -EDP, +TD, +F, +T] {
  def prepareTraining(params: TDP): TD
  def prepareEvaluation(params: EDP): Seq[(F, T)]
}

trait AbstractAlgorithm[-TD, -F, +T] {
  def train(trainingData: TD): BaseModel
  def predictBaseModel(baseModel: BaseModel, feature: F): T
}

trait BaseAlgorithm[-TD, -F, +T, M <: BaseModel]
extends AbstractAlgorithm[TD, F, T] {
  override def train(trainingData: TD): BaseModel
  override def predictBaseModel(baseModel: BaseModel, feature: F): T = 
    predict(baseModel.asInstanceOf[M], feature)
  def predict(model: M, feature: F): T
}

/*
trait BaseServer[-F, T] {
  def combine(feature: F, targets: Seq[T]): T
}

class DefaultServer[F, T] extends BaseServer[F, T] {
  override def combine(feature: F, targets: Seq[T]): T = targets.head
}
*/



