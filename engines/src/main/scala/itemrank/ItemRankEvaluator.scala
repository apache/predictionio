package io.prediction.engines.itemrank

import io.prediction.{ EvaluatorFactory }
//import io.prediction.core.AbstractEvaluator
import io.prediction.core.BaseEvaluator
//import io.prediction.core.LoalDataPreparator
import io.prediction.storage.Config
import io.prediction.storage.{ Item, U2IAction, User, ItemSet }
import io.prediction.EmptyParams
//import io.prediction.EmptyData

import io.prediction.DataPreparator
import io.prediction.Validator

import breeze.stats.{ mean, meanAndVariance }
import scala.collection.mutable.ArrayBuffer
import com.github.nscala_time.time.Imports._
import scala.math.BigDecimal

object ItemRankEvaluator extends EvaluatorFactory {

  val config = new Config
  val usersDb = config.getAppdataUsers()
  val itemsDb = config.getAppdataItems()
  val u2iDb = config.getAppdataU2IActions()
  val itemSetsDb = config.getAppdataItemSets()

  def apply() = {
    new BaseEvaluator(
      classOf[ItemRankDataPreparator],
      classOf[ItemRankValidator])
  }
}

class ItemRankDataPreparator
  extends DataPreparator[
      EvalParams,
      TrainDataPrepParams,
      ValidationDataPrepParams,
      TrainingData,
      Feature,
      Actual] {

  // Connection object makes the class not serializable.
  //@transient val usersDb = ItemRankEvaluator.usersDb
  //@transient val itemsDb = ItemRankEvaluator.itemsDb
  //@transient val u2iDb = ItemRankEvaluator.u2iDb
  //@transient val itemSetsDb = ItemRankEvaluator.itemSetsDb

  // Data generation
  override def getParamsSet(params: EvalParams)
  : Seq[(TrainDataPrepParams, ValidationDataPrepParams)] = {

    var testStart = params.testStart
    val testStartSeq = ArrayBuffer[DateTime]()
    val period = Period.hours(params.hours)

    while (testStart < params.testUntil) {
      testStartSeq += testStart
      testStart = testStart + period
    }
    //println(testStartSeq)

    val paramSeq = testStartSeq.toList.map { ts =>
      val trainingP = new TrainDataPrepParams(
        appid = params.appid,
        itypes = params.itypes,
        actions = params.actions,
        startUntil = Some((params.trainStart, ts))
      )
      val validateP = new ValidationDataPrepParams(
        appid = params.appid,
        itypes = params.itypes,
        startUntil = (ts, ts + period),
        goal = params.goal
      )
      (trainingP, validateP)
    }
    paramSeq
  }


  override def prepareTraining(params: TrainDataPrepParams): TrainingData = {
    val usersDb = ItemRankEvaluator.usersDb
    val itemsDb = ItemRankEvaluator.itemsDb
    val u2iDb = ItemRankEvaluator.u2iDb
    val itemSetsDb = ItemRankEvaluator.itemSetsDb

    val usersMap: Map[String, (UserTD, Int)] = usersDb.getByAppid(params.appid)
      .zipWithIndex
      .map { case (user, index) =>
        val userTD = new UserTD(uid = user.id)
        (user.id -> (userTD, index + 1))
      }.toMap

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

    val u2iActions = params.startUntil.map{ startUntil =>
      u2iDb.getByAppidAndTime(params.appid, startUntil._1,
        startUntil._2).toSeq
    }.getOrElse{
      u2iDb.getAllByAppid(params.appid).toSeq
    }

    val u2iActionsTDSeq = u2iActions
      .filter { u2i =>
        val validAction = params.actions.contains(u2i.action)
        val validUser = usersMap.contains(u2i.uid)
        val validItem = itemsMap.contains(u2i.iid)
        (validAction && validUser && validItem)
      }.map { u2i =>

        new U2IActionTD(
          uindex = usersMap(u2i.uid)._2, // map to index
          iindex = itemsMap(u2i.iid)._2,
          action = u2i.action,
          v = u2i.v,
          t = u2i.t.getMillis
        )
      }

    new TrainingData(
      users = usersMap.map { case (k, (v1, v2)) => (v2, v1) },
      items = itemsMap.map { case (k, (v1, v2)) => (v2, v1) },
      u2iActions = u2iActionsTDSeq
    )
  }

  override def prepareValidation(params: ValidationDataPrepParams):
    Seq[(Feature, Actual)] = {
    val usersDb = ItemRankEvaluator.usersDb
    val itemsDb = ItemRankEvaluator.itemsDb
    val u2iDb = ItemRankEvaluator.u2iDb
    val itemSetsDb = ItemRankEvaluator.itemSetsDb

    val usersSet: Set[String] = usersDb.getByAppid(params.appid)
      .map(_.id)
      .toSet

    val itemsSet: Set[String] = params.itypes.map { itypes =>
      itemsDb.getByAppidAndItypes(params.appid, itypes.toSeq)
    }.getOrElse {
      itemsDb.getByAppid(params.appid)
    }.map(_.id).toSet

    // TODO: what if want to rank multiple itemset in each period?
    // only use one itemSet for now (take(1))
    val itemSetsSeq = itemSetsDb.getByAppidAndTime(params.appid,
      params.startUntil._1,
      params.startUntil._2).toList

    val itemList = if (itemSetsSeq.isEmpty) List() else itemSetsSeq(0).iids

    // get u2i within startUntil time
    val userFT = u2iDb.getByAppidAndTime(params.appid,
      params.startUntil._1, params.startUntil._2).toSeq
      .filter { u2i =>
        val validAction = params.goal.contains(u2i.action)
        val validUser = usersSet.contains(u2i.uid)
        val validItem = itemsSet.contains(u2i.iid)
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
}

// VR and CVR can't be null
// will crash due to some weird exception:
// java.lang.ArrayStoreException: [Ljava.lang.Object;
class ItemRankValidator
  extends Validator[
      ValidatorParams,
      TrainDataPrepParams,
      ValidationDataPrepParams,
      Feature,
      Prediction,
      Actual,
      ValidationUnit,

      ValidationResult,
      CrossValidationResult] {

  var _params = new ValidatorParams(
    verbose = false
  )

  override def init(params: ValidatorParams): Unit = {
    _params = params
  }

  // evaluation

  override def validate(feature: Feature, predicted: Prediction,
    actual: Actual): ValidationUnit = {

    val k = feature.items.size

    new ValidationUnit(
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

  override def validateSet(
    trainDataPrepParams: TrainDataPrepParams,
    validationDataPrepParams: ValidationDataPrepParams,
    validationUnits: Seq[ValidationUnit]): ValidationResult = {
    // calcualte MAP at k

    val (algoMean, algoVariance, algoCount) = meanAndVariance(
      validationUnits.map(_.score)
    )
    val algoStdev = math.sqrt(algoVariance)
    val (baselineMean, baselineVariance, baselineCount) = meanAndVariance(
      validationUnits.map(_.baseline)
    )
    val baselineStdev = math.sqrt(baselineVariance)

    //val mean = validationUnits.map( eu => eu.score ).sum / validationUnits.size
    //val baseMean = validationUnits.map (eu => eu.baseline).sum /
    //  validationUnits.size

    if (_params.verbose) {
      val reports = validationUnits.map{ eu =>
        val flag = if (eu.baseline > eu.score) "x" else ""
        Seq(eu.f.uid, eu.f.items.mkString(","),
          eu.p.items.map(p => s"${p._1}(${printDouble(p._2)})").mkString(","),
          eu.a.items.mkString(","),
          printDouble(eu.baseline), printDouble(eu.score),
          flag)
      }.map { x => x.map(t => s"[${t}]")}

      println("result:")
      println(s"${trainDataPrepParams.startUntil}")
      println(s"${validationDataPrepParams.startUntil}")
      println("uid - basline - ranked - actual - baseline score - algo score")
      reports.foreach { r =>
        println(s"${r.mkString(" ")}")
      }
      println(s"baseline MAP@k = ${baselineMean} (${baselineStdev}), " +
        s"algo MAP@k = ${algoMean} (${algoStdev})")
    }

    new ValidationResult(
      testStartUntil = validationDataPrepParams.startUntil,
      baselineMean = baselineMean,
      baselineStdev = baselineStdev,
      algoMean = algoMean,
      algoStdev = algoStdev
    )
  }

  override def crossValidate(
    input: Seq[(TrainDataPrepParams, ValidationDataPrepParams,
      ValidationResult)]
  ): CrossValidationResult = {
    val vrSeq = input.map(_._3)
    // calculate mean of MAP@k of each validion result
    val (algoMean, algoVariance, algoCount) = meanAndVariance(
      vrSeq.map(_.algoMean)
    )
    val algoStdev = math.sqrt(algoVariance)

    val (baselineMean, baselineVariance, baseCount) = meanAndVariance(
      vrSeq.map(_.baselineMean)
    )
    val baselineStdev = math.sqrt(baselineVariance)

    if (_params.verbose) {
      vrSeq.sortBy(_.testStartUntil).foreach { vr =>
        println(s"${vr.testStartUntil}")
        println(s"baseline MAP@k = ${vr.baselineMean} (${vr.baselineStdev}), " +
          s"algo MAP@k = ${vr.algoMean} (${vr.algoStdev})")
      }
      println(s"baseline average = ${baselineMean} (${baselineStdev}), " +
        s"algo average = ${algoMean} (${algoStdev})")
    }

    new CrossValidationResult (
      baselineMean = baselineMean,
      baselineStdev = baselineStdev,
      algoMean = algoMean,
      algoStdev = algoStdev
    )
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
