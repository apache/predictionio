package io.prediction.engines.sparkexp

import io.prediction.{
  BaseEvaluationDataParams,
  BaseValidationParams,
  BaseCleanserParams,
  BaseAlgoParams,
  BaseServerParams,
  BaseFeature,
  BaseActual
}
import io.prediction.core.{
  AbstractEngine,
  AbstractEvaluator,
  ValidationSeq
}

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

object Runner {
  def main(args: Array[String]) {

    val engine = SparkExpEngine()
    val evaluator = SparkExpEvaluator()

    val algoParamSet = Seq(
      ("sample", new AP())
    )

    EvaluationWorkFlowSparkAlgo(
      "", new EDP(), new EDP(),
      null, algoParamSet, null,
      engine, evaluator
    )

  }
}


object EvaluationWorkFlowSparkAlgo{
  def apply(
    batch: String,
    // Should group below 2 into 1
    evalDataParams: BaseEvaluationDataParams,
    validationParams: BaseValidationParams,
    // Should group below 3 into 1
    cleanserParams: BaseCleanserParams,
    algoParamsList: Seq[(String, BaseAlgoParams)],
    serverParams: BaseServerParams,
    engine: AbstractEngine,
    evaluator: AbstractEvaluator) = {

    // ---------
    // data prep
    val conf = new SparkConf()
      .setAppName("EvaluationWorkFlowSparkAlgo")
    implicit val sc = new SparkContext(conf)

    val dataPrep = evaluator.dataPreparatorClass
      .getConstructor(classOf[SparkContext])
      .newInstance(sc)

    // eval to eval_params
    val evalParamsMap = dataPrep
      .getParamsSetBase(evalDataParams)
      .zipWithIndex
      .map(_.swap)
      .toMap

    val trainingDataMap = evalParamsMap.map { case (idx, (tdp, vdp)) =>
      (idx, dataPrep.prepareTrainingBase(tdp))
    }

    // -------
    // cleanse

    val cleanser = engine.cleanserClass.newInstance
    cleanser.initBase(cleanserParams)

    val cleansedDataMap = trainingDataMap.map { case (idx, td) =>
      (idx, cleanser.cleanseBase(td))
    }

    // ------
    // train

    // algo to algo_params
    val algoParamsMap = algoParamsList.zipWithIndex.map(_.swap).toMap

    val modelMap = cleansedDataMap.map { case (idx, cd) =>
      val algoModelMap = algoParamsMap
        .map { case (algoIdx, (algoName, algoParams)) =>
          val algorithm = engine.algorithmClassMap(algoName)
            .getConstructor(classOf[SparkContext])
            .newInstance(sc)
          algorithm.initBase(algoParams)
          (algoIdx, algorithm.trainBase(cd))
        }
      (idx, algoModelMap)
    }

    // ----------------
    // TODO: serving and validation
    val validationDataMap = evalParamsMap.map {
      case (idx, (tdp, vdp)) =>
        (idx, dataPrep.prepareValidationBase(vdp))
    }

    val server = engine.serverClass.newInstance
    server.initBase(serverParams)

    val algorithmMap = algoParamsMap
      .map { case (algoIdx, (algoName, algoParams)) =>
        val algorithm = engine.algorithmClassMap(algoName)
          .getConstructor(classOf[SparkContext])
          .newInstance(sc)
        algorithm.initBase(algoParams)
        (algoIdx, algorithm)
      }

    val validator = evaluator.validatorClass.newInstance

    val fapSeqMap = validationDataMap.map { case (idx, validationDataSeq) =>
      val algoModelMap = modelMap(idx)
      val fapSeq = validationDataSeq
        .asInstanceOf[ValidationSeq[BaseFeature, BaseActual]]
        .data.map { case (f, a) =>
          val pSeq = algorithmMap.map{ case (algoIdx, algorithm) =>
            val p = algorithm.predictBase(algoModelMap(algoIdx), f)
            p
          }.toSeq
        (f, a, server.combineBase(f, pSeq))
      }
      (idx, fapSeq)
    }

    val vuSeqMap = fapSeqMap.map { case (idx, fapSeq) =>
      val vuSeq = fapSeq.map { case (f, a, p) =>
        validator.validateBase(f, p, a)
      }
      (idx, vuSeq)
    }

    val vrSeq = vuSeqMap.map { case (idx, vuSeq) =>
      val (tdp, vdp) = evalParamsMap(idx)
      val vr = validator.validateSetBase(tdp, vdp, vuSeq)
      (tdp, vdp, vr)
    }.toList

    println(vrSeq)
  }
}
