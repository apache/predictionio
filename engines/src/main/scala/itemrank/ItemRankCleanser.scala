package io.prediction.engines.itemrank

import io.prediction.{ Cleanser }

class ItemRankCleanser extends Cleanser[TrainingData, CleansedData,
  CleanserParams] {

  final val CONFLICT_LATEST: String = "latest"
  final val CONFLICT_HIGHEST: String = "highest"
  final val CONFLICT_LOWEST: String = "lowest"

  var _cleanserParams = new CleanserParams(
    actions = Map(
      "view" -> Some(3),
      "like" -> Some(5),
      "conversion" -> Some(4),
      "rate" -> None
    ),
    conflict = "latest"
  )

  override def init(params: CleanserParams) = {
    _cleanserParams = params
  }

  override def cleanse(trainingData: TrainingData): CleansedData = {
    val actionsMap = _cleanserParams.actions
    val conflict = _cleanserParams.conflict

    // convert actions to ratings value
    val u2iRatings = trainingData.u2iActions
      .filter { u2i =>
        val validAction = actionsMap.contains(u2i.action)
        validAction
      }.map { u2i =>
        val rating = actionsMap(u2i.action).getOrElse(u2i.v.getOrElse(0))

        new RatingTD(
          uindex = u2i.uindex,
          iindex = u2i.iindex,
          rating = rating,
          t = u2i.t
        )
      }

    // resolve conflicts if users has rated items multiple times
    val ratingReduced = u2iRatings.groupBy(x => (x.iindex, x.uindex))
      .mapValues { v =>
        v.reduce { (a, b) =>
          resolveConflict(a, b, conflict)
        }
      }.values
      .toList

    new CleansedData(
      users = trainingData.users,
      items = trainingData.items,
      rating = ratingReduced
    )
  }

  private def resolveConflict(a: RatingTD, b: RatingTD,
    conflictParam: String) = {
    conflictParam match {
      case CONFLICT_LATEST  => if (a.t > b.t) a else b
      case CONFLICT_HIGHEST => if (a.rating > b.rating) a else b
      case CONFLICT_LOWEST  => if (a.rating < b.rating) a else b
    }
  }
}
