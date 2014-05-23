package io.prediction.core

trait AbstractEvaluator {
  // Data Preparation methods
  def prepareDataBase(params: BaseEvaluationParams)
    : Seq[(BaseTrainingData, BaseEvaluationSeq)]

  // Evaluation methods
  def initBase(params: BaseEvaluationParams): Unit
  
  def evaluateSeq(predictionSeq: BasePredictionSeq): BaseEvaluationUnitSeq

  def report(evalUnitSeq: BaseEvaluationUnitSeq): BaseEvaluationResults
}


trait AbstractCleanser {

  def initBase(baseCleanserParams: BaseCleanserParams): Unit

  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData

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

