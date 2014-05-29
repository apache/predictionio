package io.prediction.engines.itemrank

import io.prediction.{ Evaluator, BaseEvaluationResults }
import io.prediction.{ EvaluatorFactory }
import io.prediction.core.AbstractEvaluator
import io.prediction.storage.Config
import io.prediction.storage.{ Item, U2IAction, User, ItemSet }

import scala.collection.mutable.ArrayBuffer
import com.github.nscala_time.time.Imports._
import scala.math.BigDecimal

object ItemRankEvaluator extends EvaluatorFactory {

  val config = new Config
  val usersDb = config.getAppdataUsers
  val itemsDb = config.getAppdataItems
  val u2iDb = config.getAppdataU2IActions
  val itemSetsDb = config.getAppdataItemSets

  override def apply(): AbstractEvaluator = {
    new ItemRankEvaluator
  }
}

class ItemRankEvaluator
  extends Evaluator[EvalParams, TrainDataPrepParams, EvalDataPrepParams,
      TrainigData, Feature, Prediction, Actual, EvalUnit, EvalResults] {

  final val CONFLICT_LATEST: String = "latest"
  final val CONFLICT_HIGHEST: String = "highest"
  final val CONFLICT_LOWEST: String = "lowest"

  val usersDb = ItemRankEvaluator.usersDb
  val itemsDb = ItemRankEvaluator.itemsDb
  val u2iDb = ItemRankEvaluator.u2iDb
  val itemSetsDb = ItemRankEvaluator.itemSetsDb

  // Data generation
  override def getParamsSet(params: EvalParams): Seq[(TrainDataPrepParams,
    EvalDataPrepParams)] = {

    var testStart = params.testStart
    val testStartSeq = ArrayBuffer[DateTime]()
    val period = Period.hours(params.hours)
    while (testStart < params.testUntil) {
      testStartSeq += testStart
      testStart = testStart + period
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
        startUntil = (ts, ts + period),
        goal = params.goal
      )
      (trainingP, evalP)
    }
    paramSeq
  }


  override def prepareTraining(params: TrainDataPrepParams): TrainigData = {
    val usersMap: Map[String, Int] = usersDb.getByAppid(params.appid)
      .map(_.id).zipWithIndex
      .map { case (uid, index) => (uid, index + 1) }.toMap

    val itemsMap: Map[String, (ItemTD, Int)] = params.itypes.map { itypes =>
      itemsDb.getByAppidAndItypes(params.appid, itypes.toSeq)
    }.getOrElse {
      itemsDb.getByAppid(params.appid)
    }.zipWithIndex.map {
      case (item, index) =>
        val itemTD = new ItemTD(
          iid = item.id,
          itypes = item.itypes,
          starttime = item.starttime.map[Long](_.getMillis()),
          endtime = item.endtime.map[Long](_.getMillis()),
          inactive = item.inactive.getOrElse(false)
        )
        (item.id -> (itemTD, index + 1))
    }.toMap

    // NOTE: only contain valid items (eg. valid starttime and endtime,
    // inactive=false)
    /*
    val possibleItems: Set[Int] = itemsMap.filter {
      case (iid, (itemTD, iindex)) =>
        val validTime = itemTimeFilter(true,
          itemTD.starttime, itemTD.endtime,
          params.recommendationTime)

        validTime && (!itemTD.inactive)
    }.map {
      case (iid, (itemTD, iindex)) => iindex
    }.toSet
    */

    val u2iActions = params.startUntil.map{ startUntil =>
      u2iDb.getByAppidAndTime(params.appid, startUntil._1,
        startUntil._2).toSeq
    }.getOrElse{
      u2iDb.getAllByAppid(params.appid).toSeq
    }

    val u2iRatings = u2iActions
      .filter { u2i =>
        val validAction = params.actions.contains(u2i.action)
        val validUser = usersMap.contains(u2i.uid)
        val validItem = itemsMap.contains(u2i.iid)
        (validAction && validUser && validItem)
      }.map { u2i =>
        val rating = params.actions(u2i.action).getOrElse(u2i.v.getOrElse(0))

        new RatingTD(
          uindex = usersMap(u2i.uid), // map to index
          iindex = itemsMap(u2i.iid)._2,
          rating = rating,
          t = u2i.t.getMillis
        )
      }

    val ratingReduced = u2iRatings.groupBy(x => (x.iindex, x.uindex))
      .mapValues { v =>
        v.reduce { (a, b) =>
          resolveConflict(a, b, params.conflict)
        }
      }.values
      .toSeq

    /* write u2i seen */
    val u2iSeen = u2iActions
      .filter { u2i =>
        val validAction = params.seenActions.map(seenActions =>
          seenActions.contains(u2i.action)).getOrElse(
          // same as training actions if seenActions is not defined
          params.actions.contains(u2i.action))

        val validUser = usersMap.contains(u2i.uid)
        val validItem = itemsMap.contains(u2i.iid)
        (validAction && validUser && validItem)
      }
      // convert to index
      .map { u2i => (usersMap(u2i.uid), itemsMap(u2i.iid)._2) }
      .toSet

    new TrainigData(
      users = usersMap.map { case (k, v) => (v, k) },
      items = itemsMap.map { case (k, (v1, v2)) => (v2, v1) },
      //possibleItems = possibleItems,
      rating = ratingReduced,
      seen = u2iSeen
    )
  }

  // TODO: use t to generate eval data
  override def prepareEvaluation(params: EvalDataPrepParams):
    Seq[(Feature, Actual)] = {

    val usersMap: Map[String, Int] = usersDb.getByAppid(params.appid)
      .map(_.id).zipWithIndex
      .map { case (uid, index) => (uid, index + 1) }.toMap

    val itemsMap: Map[String, (ItemTD, Int)] = params.itypes.map { itypes =>
      itemsDb.getByAppidAndItypes(params.appid, itypes.toSeq)
    }.getOrElse {
      itemsDb.getByAppid(params.appid)
    }.zipWithIndex.map {
      case (item, index) =>
        val itemTD = new ItemTD(
          iid = item.id,
          itypes = item.itypes,
          starttime = item.starttime.map[Long](_.getMillis()),
          endtime = item.endtime.map[Long](_.getMillis()),
          inactive = item.inactive.getOrElse(false)
        )
        (item.id -> (itemTD, index + 1))
    }.toMap


    // TODO: what if want to rank multiple itemset in each period?
    // only use one itemSet for now (take(1))
    val itemList = itemSetsDb.getByAppidAndTime(params.appid,
      params.startUntil._1,
      params.startUntil._2).toList(0).iids

    // get u2i within startUntil time
    val userFT = u2iDb.getByAppidAndTime(params.appid,
      params.startUntil._1, params.startUntil._2).toSeq
      .filter { u2i =>
        val validAction = params.goal.contains(u2i.action)
        val validUser = usersMap.contains(u2i.uid)
        val validItem = itemsMap.contains(u2i.iid)
        (validAction && validUser && validItem)
      }.groupBy { u2i => u2i.uid }
      .mapValues { listOfU2i => listOfU2i.map(_.iid).toSet }
      .toSeq.sortBy(_._1)
      .map{ case (uid, iids) =>
        val f = new Feature(
          uid = uid,
          items = itemList
        )
        val t = new Actual(
          items = iids.toSeq
        )
        (f, t)
      }
    userFT
  }

  private def resolveConflict(a: RatingTD, b: RatingTD,
    conflictParam: String) = {
    conflictParam match {
      case CONFLICT_LATEST  => if (a.t > b.t) a else b
      case CONFLICT_HIGHEST => if (a.rating > b.rating) a else b
      case CONFLICT_LOWEST  => if (a.rating < b.rating) a else b
    }
  }

  private def itemTimeFilter(enable: Boolean, starttime: Option[Long],
    endtime: Option[Long], recTime: Long): Boolean = {
    if (enable) {
      (starttime, endtime) match {
        case (Some(start), None) => (recTime >= start)
        case (Some(start), Some(end)) => ((recTime >= start) &&
          (recTime < end))
        case (None, Some(end)) => (recTime < end)
        case (None, None)      => true
      }
    } else true
  }

  // evaluation

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
    println(s"baseline MAP@k = ${baseMean}, algo MAP@k = ${mean}")
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
