package io.prediction.engines.itemrank

import io.prediction.{ DataPreparator, EvaluationPreparator }

import io.prediction.storage.Config
import io.prediction.storage.{ Item, U2IAction, User }

class ItemRankDataPreparator extends DataPreparator[TrainDataParams, TrainigData]
    with EvaluationPreparator[EvalDataParams, Feature, Target] {

  final val CONFLICT_LATEST: String = "latest"
  final val CONFLICT_HIGHEST: String = "highest"
  final val CONFLICT_LOWEST: String = "lowest"

  val config = new Config
  val usersDb = config.getAppdataUsers
  val itemsDb = config.getAppdataItems
  val u2iDb = config.getAppdataU2IActions

  override def prepareTraining(params: TrainDataParams): TrainigData = {
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

    val u2iActions = u2iDb.getAllByAppid(params.appid).toSeq

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

  override def prepareEvaluation(params: EvalDataParams): Seq[(Feature, Target)] = {
    // TODO: should read test data from db as well. hardcode in param for now
    params.testUsers.map { u =>
      val f = new Feature(
        uid = u,
        items = params.testItems
      )
      // TODO
      val t = new Target(
        items = params.testItems.map(i => (i, 0.0)).toSeq
      )
      (f, t)
    }.toSeq
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
}
