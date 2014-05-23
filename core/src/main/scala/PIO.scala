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

// Runner is deprecated. Use workflow.EvaluationWorkflow instead

/*
class PIORunner(
    evalParams: BaseEvaluationParams,
    algoParams: (String, BaseAlgoParams),
    serverParams: BaseServerParams,
    engine: AbstractEngine,
    evaluator: AbstractEvaluator,
    evaluationPreparator: AbstractEvaluationPreparator) {

  def run() = {
    val verbose = 1

    val paramsIdxList = evaluator.getParamsSetBase(evalParams)
      //.take(3)  // for fast debug
      .zipWithIndex

    if (verbose > 0) {
      paramsIdxList.foreach {
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

    // TODO(yipjustin). Server is not implemented.
    server.initBase(serverParams)
    println(serverParams)

    val parParamsIdxList = (if (false) paramsIdxList else paramsIdxList.par)

    // Data Prep
    val rawDataMapPar = parParamsIdxList.map {
      case (params, idx) => {
        val (trainingParams, evaluationParams) = params
        val trainingData = dataPreparator.prepareTrainingBase(trainingParams)
        val evalSeq = evaluationPreparator.prepareEvaluationBase(evaluationParams)
        (idx, (trainingData, evalSeq))
      }
    }.toMap

    // Model Con
    val modelMapPar = rawDataMapPar.map {
      case (idx, data) => {
        (idx, algorithm.trainBase(data._1))
      }
    }.toMap

    // Serving
    val evalMapPar = rawDataMapPar.mapValues(_._2)
    val resultListPar = evalMapPar.map { case (idx, evalSeq) => {
      val model = modelMapPar(idx)

      val predictionSeq = algorithm.predictSeqBase(model, evalSeq)
      (idx, predictionSeq)
    }}

    // Evaluate
    val evalResultMap = resultListPar.seq.map { case (idx, predictionSeq) => {
      val evalUnits = evaluator.evaluateSeqBase(predictionSeq) 
      evaluator.reportBase(evalUnits)
    }}
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
      SP <: BaseServerParams,
      TDP <: BaseTrainingDataParams,
      EDP <: BaseEvaluationDataParams,
      TD <: BaseTrainingData,
      F <: BaseFeature,
      P <: BasePrediction,
      A <: BaseActual,
      EU <: BaseEvaluationUnit,
      ER <: BaseEvaluationResults
      ](
    evalParams: EP,
    algoParams: (String, AP),
    serverParams:SP,
    engine: BaseEngine[TDP, TD, F, P],
    evaluator: BaseEvaluator[EP, TDP, EDP, F, P, A, EU, ER],
    evaluationPreparator: BaseEvaluationPreparator[EDP, F, A]) = {
    val runner = new PIORunner(evalParams, algoParams, serverParams, engine, evaluator,
      evaluationPreparator)

    runner.run
  }

  // Engine is passed via serialized object.
  def apply[EP <: BaseEvaluationParams, AP <: BaseAlgoParams, TDP <: BaseTrainingDataParams, EDP <: BaseEvaluationDataParams, TD <: BaseTrainingData, F <: BaseFeature, T <: BaseTarget](
    evalParams: EP,
    algoParams: (String, AP),
    engineFilename: String,
    evaluator: BaseEvaluator[EP, TDP, EDP, F, T],
    evaluationPreparator: BaseEvaluationPreparator[EDP, F, T]) = {
    val engine = readEngine(engineFilename)
    val runner = new PIORunner(evalParams, algoParams, engine, evaluator,
      evaluationPreparator)

    runner.run
  }
}
*/
