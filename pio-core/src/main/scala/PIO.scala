package io.prediction
import com.github.nscala_time.time.Imports.DateTime

object PIOSettings {
  val appid = 42
}

object PIORunner {
  def run[
    EP <: BaseEvaluationParams,
    TDP <: BaseTrainingDataParams,
    EDP <: BaseEvaluationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    T <: BaseTarget,
    M <: BaseModel
    ](
    evalParams: EP,
    dataPreparator: BaseDataPreparator[TDP, EDP, TD, F, T],
    algorithm: AbstractAlgorithm[TD, F, T],
    server: BaseServer[M, F, T],
    evaluator: BaseEvaluator[EP, TDP, EDP, F, T]
    ) {
    val verbose = 1

    val paramsIdxList = evaluator.getParamsSet(evalParams)
      .take(3)  // for fast debug
      .zipWithIndex

      if (verbose > 0) { 
      paramsIdxList.foreach{ 
        case (data, idx) => println(s"$idx :${data._1} ${data._2}")
      }
    }
    
    // Data Prep
    val rawDataMapPar = paramsIdxList.par.map{ case(params, idx) => {
      val (trainingParams, evaluationParams) = params
      val trainingData = dataPreparator.prepareTraining(trainingParams)
      val evalDataSeq = dataPreparator.prepareEvaluation(evaluationParams)
      (idx, (trainingData, evalDataSeq))
    }}.toMap

    // Model Con
    val modelMapPar = rawDataMapPar.map{ case(idx, data) => {
      (idx, algorithm.train(data._1))
    }}.toMap

    // Serving
    val resultListPar = rawDataMapPar.map{ case(idx, data) => {
      val evalDataSeq = data._2
      val model = modelMapPar(idx)
      evalDataSeq.map{ case(evalData) => {
        val (feature, actual) = evalData
        val predicted = algorithm.predictBaseModel(model, feature)
        (idx, feature, predicted, actual)  
      }}
    }}.flatten
      
    // Evaluate
    resultListPar.seq.map{ case(idx, feature, predicted, actual) => 
      evaluator.evaluate(feature, predicted, actual)
    }
    evaluator.report
  }
}

