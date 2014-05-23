package io.prediction.core

/* Evaluator */

/*
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
*/

trait BaseEvaluator[
    -EP <: BaseEvaluationParams,
    -TD <: BaseTrainingData,
    -F <: BaseFeature,
    -P <: BasePrediction,
    -A <: BaseActual,
    EU <: BaseEvaluationUnit,
    ER <: BaseEvaluationResults
    ]
  extends AbstractEvalPrepatator {
  override def prepareDataBase(params: BaseEvaluationParams)
  : Seq[(BaseTrainingData, BaseEvaluationSeq)] = {
    val data = prepareData(params.asInstanceOf[EP])
    data.map(e => (e, new BaseEvaluationSeq(data = e._2)))
  }

  def prepareData(params: EP): Seq[(TD, Seq[(F, A)]]

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

/* DataPrepatator */

/*
trait BaseDataPreparator[-TDP, +TD <: BaseTrainingData]
  extends AbstractDataPreparator {

  override def prepareTrainingBase(params: BaseTrainingDataParams): TD =
    prepareTraining(params.asInstanceOf[TDP])

  def prepareTraining(params: TDP): TD

}
*/

/* EvaluationPrepatator */

/*
trait BaseEvaluationPreparator[-EDP, +F <: BaseFeature, +A <: BaseActual]
  extends AbstractEvaluationPreparator {

  override def prepareEvaluationBase(params: BaseEvaluationDataParams)
    : BaseEvaluationSeq = {
    val data = prepareEvaluation(params.asInstanceOf[EDP])
    new EvaluationSeq[F, A](data = data)
  }

  def prepareEvaluation(params: EDP): Seq[(F, A)]
}
*/

trait BaseCleanser[
    -TD <: BaseTrainingData,
    +CD <: BaseCleansedData,
    CP <: BaseCleanserParams]
  extends AbstractPreprocessor {

  override def initBase(params: BasePreprocessorParam): Unit = {
    init(params.asInstanceOf[PP])
  }

  def init(params: PP): Unit
  
  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData = {
    preprocess(trainingData.asInstanceOf[TD])
  }

  def cleanse(trainingData: TD): CP
}

/* Algorithm */

    //-TD <: BaseTrainingData,
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
    TDP <: BaseTrainingDataParams,
    CP <: BaseCleanserParams,
    TD <: BaseTrainingData,
    CD <: BaseClensedData,
    F <: BaseFeature,
    P <: BasePrediction](
    cleanserClass: Class[_ <: BaseCleanser[TD, CD, CP]],
    algorithmClassMap:
      Map[String,
        Class[_ <:
          BaseAlgorithm[CD, F, P, _ <: BaseModel, _ <: BaseAlgoParams]]],
    serverClass: Class[_ <: BaseServer[F, P, _ <: BaseServerParams]])
  extends AbstractEngine(dataPreparatorClass, algorithmClassMap, serverClass) {
}
