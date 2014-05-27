package io.prediction.core

// FIXME(yipjustin). I am being lazy...
import io.prediction._

trait BaseEvaluator[
    EP <: BaseEvaluationParams,
    TDP <: BaseTrainingDataParams,
    EDP <: BaseEvaluationDataParams,
    +TD <: BaseTrainingData,
    F <: BaseFeature,
    P <: BasePrediction,
    A <: BaseActual,
    EU <: BaseEvaluationUnit,
    ER <: BaseEvaluationResults
    ]
  extends AbstractEvaluator {
  
  override def getParamsSetBase(params: BaseEvaluationParams): Seq[(TDP, EDP)] 
    = getParamsSet(params.asInstanceOf[EP])

  def getParamsSet(params: EP): Seq[(TDP, EDP)]

  override def prepareTrainingBase(params: BaseTrainingDataParams): TD
    = prepareTraining(params.asInstanceOf[TDP])

  def prepareTraining(params: TDP): TD

  override def prepareEvaluationBase(params: BaseEvaluationDataParams)
    : BaseEvaluationSeq = {
    val data = prepareEvaluation(params.asInstanceOf[EDP])
    new EvaluationSeq[F, A](data = data)
  }

  def prepareEvaluation(params: EDP): Seq[(F, A)]

  override def initBase(params: BaseEvaluationParams): Unit =
    init(params.asInstanceOf[EP])

  def init(params: EP): Unit = {}

  override def evaluateSeq(predictionSeq: BasePredictionSeq)
    : BaseEvaluationUnitSeq = {
    val input: Seq[(F, P, A)] = predictionSeq
      .asInstanceOf[PredictionSeq[F, P, A]].data
    val output = input.map(e => evaluate(e._1, e._2, e._3))
    return new EvaluationUnitSeq(data = output)
  }

  def evaluate(feature: F, predicted: P, actual: A): EU

  def report(evalUnitSeq: BaseEvaluationUnitSeq): BaseEvaluationResults = {
    report(evalUnitSeq.asInstanceOf[EvaluationUnitSeq[EU]].data)
  }

  def report(evalUnits: Seq[EU]): ER
}

trait BaseCleanser[
    -TD <: BaseTrainingData,
    +CD <: BaseCleansedData,
    CP <: BaseCleanserParams]
  extends AbstractCleanser {

  override def initBase(params: BaseCleanserParams): Unit = {
    init(params.asInstanceOf[CP])
  }

  def init(params: CP): Unit
  
  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData = {
    cleanse(trainingData.asInstanceOf[TD])
  }

  def cleanse(trainingData: TD): CD
}

/* Algorithm */

trait BaseAlgorithm[
    -CD <: BaseCleansedData,
    -F <: BaseFeature,
    +P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams]
  extends AbstractAlgorithm {

  override def initBase(baseAlgoParams: BaseAlgoParams): Unit =
    init(baseAlgoParams.asInstanceOf[AP])

  def init(algoParams: AP): Unit = {}

  override def trainBase(cleansedData: BaseCleansedData): BaseModel =
    train(cleansedData.asInstanceOf[CD])

  def train(cleansedData: CD): M

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

/* Server */

trait BaseServer[-F <: BaseFeature, P <: BasePrediction, SP <: BaseServerParams]
    extends AbstractServer {

  override def initBase(baseServerParams: BaseServerParams): Unit =
    init(baseServerParams.asInstanceOf[SP])

  def init(serverParams: SP): Unit = {}

  def combineSeqBase(basePredictionSeqSeq: Seq[BasePredictionSeq])
    : BasePredictionSeq = {
    val dataSeq: Seq[Seq[(F, P, BaseActual)]] = basePredictionSeqSeq
      .map(_.asInstanceOf[PredictionSeq[F, P, BaseActual]].data).transpose

    val output: Seq[(F, P, BaseActual)] = dataSeq.map{ input => {
      val f = input(0)._1
      val ps = input.map(_._2)
      val a = input(0)._3
      // TODO(yipjustin). Check all seqs have the same f and a
      val p = combine(f, ps)
      (f, p, a)
    }} 
    new PredictionSeq[F, P, BaseActual](data = output)
  }

  def combine(feature: F, predictions: Seq[P]): P

}

/* Engine */

class BaseEngine[
    TD <: BaseTrainingData,
    CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction](
    cleanserClass: Class[_ <: BaseCleanser[TD, CD, _ <: BaseCleanserParams]],
    algorithmClassMap:
      Map[String,
        Class[_ <:
          BaseAlgorithm[CD, F, P, _ <: BaseModel, _ <: BaseAlgoParams]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]])
  extends AbstractEngine(cleanserClass, algorithmClassMap, serverClass) {
}
