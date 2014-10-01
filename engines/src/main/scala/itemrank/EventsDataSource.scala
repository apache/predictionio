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

package io.prediction.engines.itemrank

import io.prediction.engines.base

import org.joda.time.DateTime

case class EventsDataSourceParams(
  val appId: Int,
  // default None to include all itypes
  val itypes: Option[Set[String]] = None, // train items with these itypes
  // actions for training
  val actions: Set[String],
  val startTime: Option[DateTime] = None, // event starttime
  val untilTime: Option[DateTime] = None, // event untiltime
  val attributeNames: base.AttributeNames,
  override val slidingEval: Option[base.EventsSlidingEvalParams] = None
) extends base.AbstractEventsDataSourceParams

class EventsDataSource(dsp: EventsDataSourceParams)
  extends base.EventsDataSource[DataParams, Query, Actual](dsp) {

  override def generateQueryActualSeq(
    users: Map[Int, base.UserTD],
    items: Map[Int, base.ItemTD],
    actions: Seq[base.U2IActionTD],
    trainUntil: DateTime,
    evalStart: DateTime,
    evalUntil: DateTime): (DataParams, Seq[(Query, Actual)]) = {

    val ui2uid: Map[Int, String] = users.mapValues(_.uid)
    val ii2iid: Map[Int, String] = items.mapValues(_.iid)


    val userActions: Map[Int, Seq[base.U2IActionTD]] = 
      actions.groupBy(_.uindex)

    val qaSeq: Seq[(Query, Actual)] = userActions.map { case (ui, actions) => {
      val uid = ui2uid(ui)
      val iids = actions.map(u2i => ii2iid(u2i.iindex))

      val iidstr = iids.mkString(",")
      //println(s"uid: $uid iids: (${iids.size}) $iidstr")

      val actionTuples = iids.zip(actions).map(e => (uid, e._1, e._2))
      
      (Query(uid = uid, iids = iids), Actual(actionTuples = actionTuples))
    }}
    .toSeq

    (new DataParams(trainUntil, evalStart, evalUntil), qaSeq)
  }
}
