package io.prediction.engines.itemrank

import io.prediction.{ Evaluator, BaseEvaluationResults }
import scala.collection.mutable.ArrayBuffer
import com.github.nscala_time.time.Imports._
import scala.math.BigDecimal

class ItemRankEvaluator
  extends Evaluator[EvalParams, TrainDataPrepParams, EvalDataPrepParams,
      Feature, Prediction, Actual, EvalUnit, EvalResults] {

  override def getParamsSet(params: EvalParams): Seq[(TrainDataPrepParams,
    EvalDataPrepParams)] = {

    var testStart = params.testStart
    val testStartSeq = ArrayBuffer[DateTime]()
    while (testStart < params.testUntil) {
      testStartSeq += testStart
      testStart = testStart + params.period
    }

    val paramSeq = testStartSeq.toList.map { ts =>
      val trainingP = new TrainDataPrepParams(
        appid = params.appid,
        itypes = params.itypes,
        actions = params.actions,
        conflict = params.conflict,
        seenActions = params.seenActions,
        startUntil = Some((params.trainStart, ts))
      )
      val evalP = new EvalDataPrepParams(
        appid = params.appid,
        itypes = params.itypes,
        startUntil = (ts, ts + params.period),
        goal = params.goal
      )
      (trainingP, evalP)
    }
    paramSeq
  }

  override def evaluate(feature: Feature, predicted: Prediction,
    actual: Actual): EvalUnit = {

    val k = feature.items.size

    new EvalUnit(
      f = feature,
      p = predicted,
      a = actual,
      score = averagePrecisionAtK(k, predicted.items.map(_._1),
        actual.items.toSet),
      baseline = averagePrecisionAtK(k, feature.items,
        actual.items.toSet))
  }

  private def printDouble(d: Double): String = {
    BigDecimal(d).setScale(4, BigDecimal.RoundingMode.HALF_UP).toString
  }
  override def report(evalUnits: Seq[EvalUnit]): EvalResults = {
    // calcualte MAP at k
    val mean = evalUnits.map( eu => eu.score ).sum / evalUnits.size
    val baseMean = evalUnits.map (eu => eu.baseline).sum / evalUnits.size
    // TODO: simply print results for now...
    val reports = evalUnits.map{ eu =>
      val flag = if (eu.baseline > eu.score) "x" else ""
      Seq(eu.f.uid, eu.f.items.mkString(","),
      eu.p.items.map(_._1).mkString(","),
       eu.a.items.mkString(","),
       printDouble(eu.baseline), printDouble(eu.score),
       flag)
    }.map { x => x.map(t => s"[${t}]")}

    println("result:")
    println("uid - basline - ranked - actual - baseline - score")
    reports.foreach { r =>
      println(s"${r.mkString(" ")}")
    }
    println(s"baseline MAP@k = ${baseMean}, MAP@k = ${mean}")
    new EvalResults()
  }

  // metric
  private def averagePrecisionAtK[T](k: Int, p: Seq[T], r: Set[T]): Double = {
    // supposedly the predictedItems.size should match k
    // NOTE: what if predictedItems is less than k? use the avaiable items as k.
    val n = scala.math.min(p.size, k)

    // find if each element in the predictedItems is one of the relevant items
    // if so, map to 1. else map to 0
    // (0, 1, 0, 1, 1, 0, 0)
    val rBin: Seq[Int] = p.take(n).map { x => if (r(x)) 1 else 0 }
    val pAtKNom = rBin.scanLeft(0)(_ + _)
      .drop(1) // drop 1st one which is initial 0
      .zip(rBin)
      .map(t => if (t._2 != 0) t._1.toDouble else 0.0)
    // ( number of hits at this position if hit or 0 if miss )

    val pAtKDenom = 1 to rBin.size
    val pAtK = pAtKNom.zip(pAtKDenom).map { t => t._1 / t._2 }
    val apAtKDenom = scala.math.min(n, r.size)
    if (apAtKDenom == 0) 0 else pAtK.sum / apAtKDenom
  }

}
