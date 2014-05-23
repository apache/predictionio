package io.prediction.core

trait AbstractEvalPrepatator {
  // Data Preparation methods
  /*
  def getParamsSetBase(params: BaseEvaluationParams)
    : Seq[(BaseTrainingDataParams, BaseEvaluationDataParams)]

  def prepareTrainingBase(params: BaseTrainingDataParams): BaseTrainingData

  def prepareEvaluationBase(params: BaseEvaluationDataParams): BaseEvaluationSeq
  */
  def prepareDataBase(params: BaseEvaluationParams)
    : Seq[(BaseTrainingData, BaseEvaluationSeq)]
}

trait AbstractEvaluator {
  // Evaluation methods
  def initBase(params: BaseEvaluationParams): Unit
  
  def evaluateSeq(predictionSeq: BasePredictionSeq): BaseEvaluationUnitSeq

  def report(evalUnitSeq: BaseEvaluationUnitSeq): BaseEvaluationResults
}


trait AbstractPreprocessor {

  def initBase(basePreprocessParams: BasePreprocessParams): Unit

  def preprocessBase(trainingData: BaseTrainingData): BaseProcessedData

}


trait AbstractAlgorithm {

  def initBase(baseAlgoParams: BaseAlgoParams): Unit

  def trainBase(processedData: BaseProcessedData): BaseModel

  def predictSeqBase(baseModel: BaseModel, evalSeq: BaseEvaluationSeq)
    : BasePredictionSeq

}

trait AbstractServer {

  def initBase(baseServerParams: BaseServerParams): Unit

  // The server takes a seq of Prediction and combine it into one.
  // In the batch model, things are run in batch therefore we have seq of seq.
  def combineSeqBase(basePredictionSeqSeq: Seq[BasePredictionSeq])
    : BasePredictionSeq
}

class AbstractEngine(

  val preprocessorClass: Class[_ <: AbstractPreprocessor],

  val algorithmClassMap: Map[String, Class[_ <: AbstractAlgorithm]],

  val serverClass: Class[_ <: AbstractServer]) {

}

