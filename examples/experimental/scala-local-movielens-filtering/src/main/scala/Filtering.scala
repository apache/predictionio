/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package myorg

import org.apache.predictionio.controller.LServing
import org.apache.predictionio.controller.Params
import org.apache.predictionio.engines.itemrec.Prediction
import org.apache.predictionio.engines.itemrec.Query
import scala.io.Source

case class TempFilterParams(val filepath: String) extends Params

class TempFilter(val params: TempFilterParams) 
    extends LServing[TempFilterParams, Query, Prediction] {
  override def serve(query: Query, predictions: Seq[Prediction]): Prediction = {
    val disabledIids: Set[String] = Source.fromFile(params.filepath)
      .getLines()
      .toSet

    val prediction = predictions.head
    // prediction.items is a list of (item_id, score)-tuple
    prediction.copy(items = prediction.items.filter(e => !disabledIids(e._1)))
  }
}
