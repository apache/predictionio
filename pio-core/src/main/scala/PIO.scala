package io.prediction
import com.github.nscala_time.time.Imports.DateTime

object PIOSettings {
  val appid = 42
}

object PIORunner {
  def run[
    EP <: BaseEvaluationParams,
    AP <: BaseAlgoParams,
    TDP <: BaseTrainingDataParams,
    EDP <: BaseEvaluationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    T <: BaseTarget,
    M <: BaseModel
    ](
    evalParams: EP,
    algoParams: (String, AP),
    engine: BaseEngine[TDP, TD, F, T],
    evaluator: BaseEvaluator[EP, TDP, EDP, F, T],
    evaluationPreparator: BaseEvaluationPreparator[EDP, F, T]
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

    // Init engine
    val dataPreparator = engine.dataPreparatorClass.newInstance
    val algoMap = engine.algorithmClassMap.mapValues(cls => cls.newInstance)
    val algorithm = algoMap(algoParams._1)
    val server = engine.serverClass.newInstance

    algorithm.initBase(algoParams._2)
    println(algoParams)
    
    // Data Prep
    val rawDataMapPar = paramsIdxList/*.par*/.map{ case(params, idx) => {
      val (trainingParams, evaluationParams) = params
      val trainingData = dataPreparator.prepareTraining(trainingParams)
      val evalDataSeq = evaluationPreparator.prepareEvaluation(evaluationParams)
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

