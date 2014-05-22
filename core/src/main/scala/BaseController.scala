package io.prediction

trait AbstractEvaluator {

  def getParamsSetBase(params: BaseEvaluationParams):
    Seq[(BaseTrainingDataParams, BaseEvaluationDataParams)]

  def evaluateBase(
    feature: BaseFeature,
    predicted: BasePrediction,
    actual: BaseActual): BaseEvaluationUnit

  def reportBase(evalUnits: Seq[BaseEvaluationUnit]): BaseEvaluationResults

}

trait BaseEvaluator[
    -EP <: BaseEvaluationParams,
    +TDP <: BaseTrainingDataParams,
    +EDP <: BaseEvaluationDataParams,
    -F <: BaseFeature,
    -P <: BasePrediction,
    -A <: BaseActual,
    EU <: BaseEvaluationUnit,
    ER <: BaseEvaluationResults
    ]
  extends AbstractEvaluator {

  override def getParamsSetBase(params: BaseEvaluationParams): Seq[(TDP, EDP)] =
    getParamsSet(params.asInstanceOf[EP])

  def getParamsSet(params: EP): Seq[(TDP, EDP)]

  override def evaluateBase(
    feature: BaseFeature,
    predicted: BasePrediction,
    actual: BaseActual): BaseEvaluationUnit =
    evaluate(
      feature.asInstanceOf[F],
      predicted.asInstanceOf[P],
      actual.asInstanceOf[A])

  def evaluate(feature: F, predicted: P, actual: A): EU

  //override
  def reportBase(evalUnits: Seq[BaseEvaluationUnit]): BaseEvaluationResults = {
    report(evalUnits.map(_.asInstanceOf[EU]))
  }

  def report(evalUnits: Seq[EU]): ER

}

trait AbstractDataPreparator {

  def prepareTrainingBase(params: BaseTrainingDataParams): BaseTrainingData

}

trait BaseDataPreparator[-TDP, +TD <: BaseTrainingData]
  extends AbstractDataPreparator {

  override def prepareTrainingBase(params: BaseTrainingDataParams): TD =
    prepareTraining(params.asInstanceOf[TDP])

  def prepareTraining(params: TDP): TD

}

trait AbstractEvaluationPreparator {

  def prepareEvaluationBase(params: BaseEvaluationDataParams):
    Seq[(BaseFeature, BaseActual)]

}

trait BaseEvaluationPreparator[-EDP, +F <: BaseFeature, +A <: BaseActual]
  extends AbstractEvaluationPreparator {

  override def prepareEvaluationBase(params: BaseEvaluationDataParams):
    Seq[(F, A)] = prepareEvaluation(params.asInstanceOf[EDP])

  def prepareEvaluation(params: EDP): Seq[(F, A)]

}

trait AbstractAlgorithm {

  def initBase(baseAlgoParams: BaseAlgoParams): Unit

  def trainBase(trainingData: BaseTrainingData): BaseModel

  def predictBase(baseModel: BaseModel, feature: BaseFeature): BasePrediction

}

trait BaseAlgorithm[
    -TD <: BaseTrainingData,
    -F <: BaseFeature,
    +P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams]
  extends AbstractAlgorithm {

  override def initBase(baseAlgoParams: BaseAlgoParams): Unit =
    init(baseAlgoParams.asInstanceOf[AP])

  def init(algoParams: AP): Unit = {}

  override def trainBase(trainingData: BaseTrainingData): BaseModel =
    train(trainingData.asInstanceOf[TD])

  def train(trainingData: TD): M

  override def predictBase(
    baseModel: BaseModel,
    feature: BaseFeature): BasePrediction =
    predict(baseModel.asInstanceOf[M], feature.asInstanceOf[F])

  def predict(model: M, feature: F): P

}

trait AbstractServer {

  def initBase(baseServerParams: BaseServerParams): Unit

  def combineBase(feature: BaseFeature,
    predictions: Seq[BasePrediction]): BasePrediction

}

trait BaseServer[-F, P <: BasePrediction, SP <: BaseServerParams]
    extends AbstractServer {

  override def initBase(baseServerParams: BaseServerParams): Unit =
    init(baseServerParams.asInstanceOf[SP])

  def init(serverParams: SP): Unit = {}

  override def combineBase(feature: BaseFeature,
    predictions: Seq[BasePrediction]) =
    combine(feature.asInstanceOf[F], predictions.map(_.asInstanceOf[P]))

  def combine(feature: F, predictions: Seq[P]): P

}

class AbstractEngine(

  val dataPreparatorClass: Class[_ <: AbstractDataPreparator],

  val algorithmClassMap: Map[String, Class[_ <: AbstractAlgorithm]],

  val serverClass: Class[_ <: AbstractServer]) {

}

class BaseEngine[
    TDP <: BaseTrainingDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    P <: BasePrediction](
    dataPreparatorClass: Class[_ <: BaseDataPreparator[TDP, TD]],
    algorithmClassMap:
      Map[String,
        Class[_ <:
          BaseAlgorithm[TD, F, P, _ <: BaseModel, _ <: BaseAlgoParams]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]])
  extends AbstractEngine(dataPreparatorClass, algorithmClassMap, serverClass) {
}
