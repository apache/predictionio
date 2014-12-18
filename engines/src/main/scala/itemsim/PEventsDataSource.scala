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

package io.prediction.engines.itemsim

import io.prediction.controller.EmptyDataParams
import io.prediction.engines.base
import io.prediction.controller.Workflow
import io.prediction.controller.WorkflowParams

class PEventsDataSource(dsp: EventsDataSourceParams)
  extends base.PEventsDataSource[EmptyDataParams, Query, Actual](dsp)

// This runner for testing purpose only
object PEventsDataSourceRunner {

  def main(args: Array[String]) {
    val dsp = EventsDataSourceParams(
      appId = args(0).toInt,
      // default None to include all itypes
      itypes = None, // train items with these itypes
      // actions for training
      actions = Set("view", "like", "dislike", "conversion", "rate"),
      startTime = None,
      untilTime = None,
      attributeNames = base.AttributeNames(
        user = "pio_user",
        item = "pio_item",
        u2iActions = Set("view", "like", "dislike", "conversion", "rate"),
        itypes = "pio_itypes",
        starttime = "pio_starttime",
        endtime = "pio_endtime",
        inactive = "pio_inactive",
        rating = "pio_rating"
      )
    )

    Workflow.run(
      dataSourceClassMapOpt = Some(Map("" -> classOf[PEventsDataSource])),
      dataSourceParams = ("", dsp),
      params = WorkflowParams(
        batch = "Itemsim PEventsDataSource",
        verbose = 3
      )
    )
  }
}
