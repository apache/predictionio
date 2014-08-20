package io.prediction.examples.itemrank

import io.prediction.controller.LDataSource
import io.prediction.storage.Storage
//import io.prediction.storage.{ Item, U2IAction, User, ItemSet }

import scala.collection.mutable.ArrayBuffer
import com.github.nscala_time.time.Imports._

class ItemRankDataSource(dsp: DataSourceParams)
  extends LDataSource[DataSourceParams,
    DataParams, TrainingData, Query, Actual] {
  
  @transient lazy val usersDb = Storage.getAppdataUsers()
  @transient lazy val itemsDb = Storage.getAppdataItems()
  @transient lazy val u2iDb = Storage.getAppdataU2IActions()
  @transient lazy val itemSetsDb = Storage.getAppdataItemSets()

  override def read(): Seq[(DataParams, TrainingData,
    Seq[(Query, Actual)])] = {
    getParamsSet(dsp).map { case (tdp, vdp) =>
      (new DataParams(tdp = tdp, vdp = vdp),
        prepareTraining(tdp),
        prepareValidation(vdp)
      )
    }
  }

  // Data generation
  private def getParamsSet(params: DataSourceParams)
  : Seq[(TrainingDataParams, ValidationDataParams)] = {

    var testStart = params.testStart
    val testStartSeq = ArrayBuffer[DateTime]()
    val period = Period.hours(params.hours)

    while (testStart < params.testUntil) {
      testStartSeq += testStart
      testStart = testStart + period
    }
    //println(testStartSeq)

    val paramSeq = testStartSeq.toList.map { ts =>
      val trainingP = new TrainingDataParams(
        appid = params.appid,
        itypes = params.itypes,
        actions = params.actions,
        startUntil = Some((params.trainStart, ts)),
        verbose = params.verbose
      )
      val validateP = new ValidationDataParams(
        appid = params.appid,
        itypes = params.itypes,
        startUntil = (ts, ts + period),
        goal = params.goal
      )
      (trainingP, validateP)
    }
    paramSeq
  }

  private def prepareTraining(params: TrainingDataParams): TrainingData = {
    if (params.verbose)
      println(params)

    val usersMap: Map[String, (UserTD, Int)] = usersDb.getByAppid(params.appid)
      .zipWithIndex
      .map { case (user, index) =>
        val userTD = new UserTD(uid = user.id)
        (user.id -> (userTD, index + 1))
      }.toMap

    if (params.verbose)
      println(usersMap.size)

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

    if (params.verbose)
      println(itemsMap.size)

    val u2iActions = params.startUntil.map{ startUntil =>
      u2iDb.getByAppidAndTime(params.appid, startUntil._1,
        startUntil._2).toList
    }.getOrElse{
      u2iDb.getAllByAppid(params.appid).toList
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

    if (params.verbose)
      println(u2iActionsTDSeq.size)

    new TrainingData(
      users = usersMap.map { case (k, (v1, v2)) => (v2, v1) },
      items = itemsMap.map { case (k, (v1, v2)) => (v2, v1) },
      u2iActions = u2iActionsTDSeq
    )
  }

  private def prepareValidation(params: ValidationDataParams):
    Seq[(Query, Actual)] = {
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
        val f = new Query(
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
