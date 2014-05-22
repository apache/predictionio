package io.prediction

trait AbstractEvaluator {

  def initBase(params: BaseEvaluationParams): Unit

  def getParamsSetBase(params: BaseEvaluationParams):
    Seq[(BaseTrainingDataParams, BaseEvaluationDataParams)]

  def evaluateSeqBase(predictionSeq: BasePredictionSeq): BaseEvaluationUnitSeq

  def reportBase(evalUnitSeq: BaseEvaluationUnitSeq): BaseEvaluationResults

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

  override def initBase(params: BaseEvaluationParams): Unit =
    init(params.asInstanceOf[EP])

  def init(params: EP): Unit = {}

  override def getParamsSetBase(params: BaseEvaluationParams): Seq[(TDP, EDP)] =
    getParamsSet(params.asInstanceOf[EP])

  def getParamsSet(params: EP): Seq[(TDP, EDP)]
  
  override def evaluateSeqBase(predictionSeq: BasePredictionSeq)
    : BaseEvaluationUnitSeq = {
    val input: Seq[(F, P, A)] = predictionSeq
      .asInstanceOf[PredictionSeq[F, P, A]].data
    val output = input.map(e => evaluate(e._1, e._2, e._3))
    return new EvaluationUnitSeq(data = output)
  }

  def evaluate(feature: F, predicted: P, actual: A): EU

  def reportBase(evalUnitSeq: BaseEvaluationUnitSeq): BaseEvaluationResults = {
    report(evalUnitSeq.asInstanceOf[EvaluationUnitSeq[EU]].data)
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

  def prepareEvaluationBase(params: BaseEvaluationDataParams): BaseEvaluationSeq

}

trait BaseEvaluationPreparator[-EDP, +F <: BaseFeature, +A <: BaseActual]
  extends AbstractEvaluationPreparator {

  override def prepareEvaluationBase(params: BaseEvaluationDataParams)
    : BaseEvaluationSeq = {
    val data = prepareEvaluation(params.asInstanceOf[EDP])
    new EvaluationSeq[F, A](data = data)
  }

  def prepareEvaluation(params: EDP): Seq[(F, A)]

}

trait AbstractAlgorithm {

  def initBase(baseAlgoParams: BaseAlgoParams): Unit

  def trainBase(trainingData: BaseTrainingData): BaseModel

  def predictSeqBase(baseModel: BaseModel, evalSeq: BaseEvaluationSeq)
    : BasePredictionSeq

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

  override def predictSeqBase(baseModel: BaseModel,
    evalSeq: BaseEvaluationSeq): BasePredictionSeq = {
   
    val input: Seq[(F, BaseActual)] = evalSeq
      .asInstanceOf[EvaluationSeq[F, BaseActual]]
      .data

    val model = baseModel.asInstanceOf[M]
    // Algorithm don't know the actual subtype used.
    val output: Seq[(F, P, BaseActual)] = input.map{ case(f, a) => {
      (f, predict(model, f), a)
    }}
    new PredictionSeq[F, P, BaseActual](data = output)
  }

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
