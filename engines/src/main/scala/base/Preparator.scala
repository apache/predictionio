/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.engines.base

import io.prediction.controller.LPreparator
import io.prediction.controller.Params

abstract class AbstractPreparatorParams extends Params {
  // how to map selected actions into rating value
  // use None if use U2IActionTD.v field
  val actions: Map[String, Option[Int]] // ((view, 1), (rate, None))
  val seenActions: Set[String] // (view, conversion)
  // conflict resolution, "latest" "highest" "lowest" "sum"
  val conflict: String
}

object Preparator {
  // Maps actions to a numerical rating with the following logic:
  // If action belongs to actionMap, then it looks at the map value. If it is
  // defined, use it; otherwise, use the numerical value v.
  // Last, if above method fails to define a numercical rating, default is used.
  def action2rating(action: String, v: Option[Int],
    actionsMap: Map[String, Option[Int]], default: Int): Int = {
    actionsMap(action).getOrElse(v.getOrElse(default))
  }

  def action2rating(u2i: U2IActionTD,
    actionsMap: Map[String, Option[Int]], default: Int): Int = {
    action2rating(u2i.action, u2i.v, actionsMap, default)
  }

  /*
  // Return a boolean value. Usually used by metrics to define "good" rating.
  // Logic is simliar to action2rating, except that a threshold will be used for
  // comparing numerical value.
  def action2boolean(action: String, v: Option[Int],
    actionsMap: Map[String, Option[Boolean]], 
    threshold: Int, default: Boolean) = {
    
    actionsMap(action).getOrElse(v.map(_ >= threshold).getOrElse(default))
  }
  */
}

class Preparator(pp: AbstractPreparatorParams) 
    extends LPreparator[TrainingData, PreparedData] {

  final val CONFLICT_LATEST: String = "latest"
  final val CONFLICT_HIGHEST: String = "highest"
  final val CONFLICT_LOWEST: String = "lowest"
  final val CONFLICT_SUM: String = "sum"

  override def prepare(trainingData: TrainingData): PreparedData = {

    val actionsMap = pp.actions
    val conflict = pp.conflict

    // convert actions to ratings value
    val u2iRatings = trainingData.u2iActions
      .filter { u2i =>
        val validAction = actionsMap.contains(u2i.action)
        validAction
      }.map { u2i =>
        //val rating = actionsMap(u2i.action).getOrElse(u2i.v.getOrElse(0))
        val rating = Preparator.action2rating(u2i.action, u2i.v,
          actionsMap, default = 0)

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

    val seenU2IActions = if (pp.seenActions.isEmpty)
      None
    else {
      Some(trainingData.u2iActions
        .filter(u2i => pp.seenActions.contains(u2i.action)))
    }

    new PreparedData(
      users = trainingData.users,
      items = trainingData.items,
      rating = ratingReduced,
      ratingOriginal = u2iRatings,
      seenU2IActions = seenU2IActions
    )
  }

  private def resolveConflict(a: RatingTD, b: RatingTD,
    conflictParam: String) = {
    conflictParam match {
      case CONFLICT_LATEST  => if (a.t > b.t) a else b
      case CONFLICT_HIGHEST => if (a.rating > b.rating) a else b
      case CONFLICT_LOWEST  => if (a.rating < b.rating) a else b
      case CONFLICT_SUM => new RatingTD(
        uindex = a.uindex,
        iindex = a.iindex,
        rating = a.rating + b.rating,
        t = math.max(a.t, b.t)) // keep latest timestamp
    }
  }
}
