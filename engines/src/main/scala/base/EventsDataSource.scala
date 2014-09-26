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

import io.prediction.controller.LDataSource
import io.prediction.controller.Params
import io.prediction.controller.EmptyDataParams
import io.prediction.data.view.LBatchView

import org.joda.time.DateTime
import org.joda.time.Duration

import scala.reflect.ClassTag

import grizzled.slf4j.Logger

abstract class AbstractEventsDataSourceParams extends Params {
  val appId: Int
  // default None to include all itypes
  val itypes: Option[Set[String]] // train items with these itypes
  // actions for trainingdata
  val actions: Set[String]
  // only consider events happening after starttime.
  val startTime: Option[DateTime] 
  // only consider events happening until untiltime.
  val untilTime: Option[DateTime] 
  // used for mapping attributes from event store.
  val attributeNames: AttributeNames
  // for generating evaluation data sets. See [[[EventsSlidingEvaluationParams]]].
  val slidingEvaluation: Option[EventsSlidingEvaluationParams] = None
}

/* Parameters for generating evaluation (testing) data.
 *
 * Generates data in a sliding window fashion. First, it sets a cutoff time for
 * training data, all events whose timestamp is less than the cutoff time go to
 * training, then it takes all events that happened between the
 * [firstCutoffTime, firstCutoffTime + evaluationDuration] as test set.
 * Afterwards, it uses events up to firstCutoffTime + evaluationDuration as
 * training set, and [firstCutoffTime + evaluationDuration, firstCutoffTime +
 * 2 x evaluationDuration] as test set. This process is repeated for
 * evaluationCount times.
 *
 * It is important to note that this sliding window is usually subjected to the
 * startTime and endTime of the parent DataSourceParams.
 */
class EventsSlidingEvaluationParams(
  val firstTrainingCutoffTime: DateTime,
  val evaluationDuration: Duration,
  val evaluationCount: Int
)

class EventsDataSource[DP: ClassTag, Q, A](
  dsp: AbstractEventsDataSourceParams)
  extends LDataSource[AbstractEventsDataSourceParams,
    DP, TrainingData, Q, A] {

  @transient lazy val logger = Logger[this.type]
  @transient lazy val batchView = new LBatchView(dsp.appId,
    dsp.startTime, dsp.untilTime)

  override
  def read(): Seq[(DP, TrainingData, Seq[(Q, A)])] = {
    val (users, items, u2iActions)
    : (Map[Int, UserTD], Map[Int, ItemTD], Seq[U2IActionTD]) = extract()

    if (dsp.slidingEvaluation.isEmpty) {
      val trainingData = new TrainingData(
        users = users,
        items = items,
        u2iActions = u2iActions)
      return Seq((null.asInstanceOf[DP], trainingData, Seq[(Q, A)]()))
    } else {
      return generateSliding(users, items, u2iActions)
    }
  }

  // sub-classes should override this method.
  def generateSliding(
    users: Map[Int, UserTD],
    items: Map[Int, ItemTD],
    u2iActions: Seq[U2IActionTD]): Seq[(DP, TrainingData, Seq[(Q, A)])] = {
    Seq[(DP, TrainingData, Seq[(Q, A)])]()
  }

  //override
  //def readTraining(): TrainingData = {

  def extract(): (Map[Int, UserTD], Map[Int, ItemTD], Seq[U2IActionTD]) = {
    val attributeNames = dsp.attributeNames
    // uid => (UserTD, uindex)
    val usersMap: Map[String, (UserTD, Int)] = batchView
      .aggregateProperties(attributeNames.user)
      .zipWithIndex
      .map { case ((entityId, dataMap), index) =>
        val userTD = new UserTD(uid = entityId)
        (entityId -> (userTD, index + 1)) // make index starting from 1
      }

    val itemsMap = batchView
      .aggregateProperties(attributeNames.item)
      .map { case (entityId, dataMap) =>
        val itemTD = try {
          new ItemTD(
            iid = entityId,
            itypes = dataMap.get[List[String]](attributeNames.itypes),
            starttime = dataMap.getOpt[DateTime](attributeNames.starttime)
              .map(_.getMillis),
            endtime = dataMap.getOpt[DateTime](attributeNames.endtime)
              .map(_.getMillis),
            inactive = dataMap.getOpt[Boolean](attributeNames.inactive)
              .getOrElse(false)
          )
        } catch {
          case exception: Exception => {
            logger.error(s"${exception}: entityType ${attributeNames.item} " +
              s"entityID ${entityId}: ${dataMap}." )
            throw exception
          }
        }
        (entityId -> itemTD)
      }.filter { case (id, (itemTD)) =>
        dsp.itypes.map{ t =>
          !(itemTD.itypes.toSet.intersect(t).isEmpty)
        }.getOrElse(true)
      }.zipWithIndex.map { case ((id, itemTD), index) =>
        (id -> (itemTD, index + 1))
      }

    val u2iActions = batchView.events
      .filter{ e =>
        attributeNames.u2iActions.contains(e.event) &&
        dsp.actions.contains(e.event) &&
        usersMap.contains(e.entityId) &&
        // if the event doesn't have targetEntityId, also include it
        // although it's error case.
        // check and flag error in next step
        e.targetEntityId.map(itemsMap.contains(_)).getOrElse(true)
      }.map { e =>
        // make sure targetEntityId exist in this event
        require((e.targetEntityId != None),
          s"u2i Event: ${e} cannot have targetEntityId empty.")
        try {
          new U2IActionTD(
            uindex = usersMap(e.entityId)._2,
            iindex = itemsMap(e.targetEntityId.get)._2,
            action = e.event,
            v = e.properties.getOpt[Int](attributeNames.rating),
            t = e.eventTime.getMillis
          )
        } catch {
          case exception: Exception => {
            logger.error(s"${exception}: event ${e}.")
            throw exception
          }
        }
      }

    /*
    new TrainingData(
      users = usersMap.map { case (k, (v1, v2)) => (v2, v1) },
      items = itemsMap.map { case (k, (v1, v2)) => (v2, v1) },
      u2iActions = u2iActions
    )
    */
    return (
      usersMap.values.map(_.swap).toMap,
      itemsMap.values.map(_.swap).toMap,
      u2iActions)
  }
}
