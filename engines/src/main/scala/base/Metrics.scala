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

import io.prediction.controller.Params
import io.prediction.controller.Metrics

// Describe how to map from actions to a binary rating parameter
class BinaryRatingParams(
  val actionsMap: Map[String, Option[Int]], // ((view, 1), (rate, None))
  val goodThreshold: Int  // action rating >= goodThreshold is good.
) extends Serializable


object MetricsHelper {
  def actions2GoodIids(
    actionTuples: Seq[(String, String, U2IActionTD)], 
    binaryRatingParams: BinaryRatingParams)
  : Set[String] = {
    actionTuples
    .filter{ t => {
      val u2i = t._3
      val rating = Preparator.action2rating(
        u2i = u2i,
        actionsMap = binaryRatingParams.actionsMap,
        default = 0)
      rating >= binaryRatingParams.goodThreshold
    }}
    .map(_._2)
    .toSet
  }
}


