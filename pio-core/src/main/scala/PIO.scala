package io.prediction
import com.github.nscala_time.time.Imports.DateTime

import com.twitter.chill.MeatLocker 
import java.io.FileOutputStream 
import java.io.ObjectOutputStream 
import java.io.FileInputStream 
import java.io.ObjectInputStream 

object PIOSettings {
  val appid = 42
}

class PIORunner(
  evalParams: BaseEvaluationParams,
  algoParams: (String, BaseAlgoParams),
  engine: AbstractEngine,
  evaluator: AbstractEvaluator,
  evaluationPreparator: AbstractEvaluationPreparator) {
  
  def run() = {
    val verbose = 1

    val paramsIdxList = evaluator.getParamsSetBase(evalParams)
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
      val trainingData = dataPreparator.prepareTrainingBase(trainingParams)
      val evalDataSeq = evaluationPreparator.prepareEvaluationBase(evaluationParams)
      (idx, (trainingData, evalDataSeq))
    }}.toMap

    // Model Con
    val modelMapPar = rawDataMapPar.map{ case(idx, data) => {
      (idx, algorithm.trainBase(data._1))
    }}.toMap

    // Serving
    val resultListPar = rawDataMapPar.map{ case(idx, data) => {
      val evalDataSeq = data._2
      val model = modelMapPar(idx)
      evalDataSeq.map{ case(evalData) => {
        val (feature, actual) = evalData
        val predicted = algorithm.predictBase(model, feature)
        (idx, feature, predicted, actual)  
      }}
    }}.flatten
      
    // Evaluate
    resultListPar.seq.map{ case(idx, feature, predicted, actual) => 
      evaluator.evaluateBase(feature, predicted, actual)
    }
    evaluator.report
  }
}


object PIORunner {
  def writeEngine(
    engine: AbstractEngine,
    filename: String) = {
    println(s"Serialize engine to $filename")
    val boxedEngine = MeatLocker(engine) 
    val oos = new ObjectOutputStream(new FileOutputStream(filename)) 
    oos.writeObject(boxedEngine)
    oos.close
  }

  def readEngine(filename: String): AbstractEngine = {
    println(s"Read engine from $filename")
    val ois = new ObjectInputStream(new FileInputStream(filename))
    val obj = ois.readObject
    val engine = obj.asInstanceOf[MeatLocker[AbstractEngine]].get
    engine
  }

  // Type checking between interfaces happens here.
  def apply[
    EP <: BaseEvaluationParams,
    AP <: BaseAlgoParams,
    TDP <: BaseTrainingDataParams,
    EDP <: BaseEvaluationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    T <: BaseTarget
    ](
    evalParams: EP,
    algoParams: (String, AP),
    engine: BaseEngine[TDP, TD, F, T],
    evaluator: BaseEvaluator[EP, TDP, EDP, F, T],
    evaluationPreparator: BaseEvaluationPreparator[EDP, F, T]
    ) = {
    val runner = new PIORunner(evalParams, algoParams, engine, evaluator,
      evaluationPreparator)

    runner.run
  }

  // Engine is passed via serialized object.
  def apply[
    EP <: BaseEvaluationParams,
    AP <: BaseAlgoParams,
    TDP <: BaseTrainingDataParams,
    EDP <: BaseEvaluationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    T <: BaseTarget
    ](
    evalParams: EP,
    algoParams: (String, AP),
    engineFilename: String,
    evaluator: BaseEvaluator[EP, TDP, EDP, F, T],
    evaluationPreparator: BaseEvaluationPreparator[EDP, F, T]
    ) = {
    val engine = readEngine(engineFilename)
    val runner = new PIORunner(evalParams, algoParams, engine, evaluator,
      evaluationPreparator)

    runner.run
  }

  @deprecated("Use PIORunner(...) instead", "since 20140517")
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
    ) = {

    PIORunner(evalParams, algoParams, engine, evaluator, evaluationPreparator)
  }
}

