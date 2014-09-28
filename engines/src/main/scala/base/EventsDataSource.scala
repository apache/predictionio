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
import scala.collection.immutable.HashMap
import scala.collection.immutable.List

import scala.language.implicitConversions 

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
  // for generating eval data sets. See [[[EventsSlidingEvalParams]]].
  val slidingEval: Option[EventsSlidingEvalParams] = None
}

/* Parameters for generating eval (testing) data.
 *
 * Generates data in a sliding window fashion. First, it sets a cutoff time for
 * training data, all events whose timestamp is less than the cutoff time go to
 * training, then it takes all events that happened between the
 * [firstUntilTime, firstUntilTime + evalDuration] as test set.
 * Afterwards, it uses events up to firstUntilTime + evalDuration as
 * training set, and [firstUntilTime + evalDuration, firstUntilTime +
 * 2 x evalDuration] as test set. This process is repeated for
 * evalCount times.
 *
 * It is important to note that this sliding window is usually subjected to the
 * startTime and endTime of the parent DataSourceParams.
 */
class EventsSlidingEvalParams(
  val firstTrainingUntilTime: DateTime,
  val evalDuration: Duration,
  val evalCount: Int
) extends Serializable

class EventsDataSource[DP: ClassTag, Q, A](
  dsp: AbstractEventsDataSourceParams)
  extends LDataSource[AbstractEventsDataSourceParams,
    DP, TrainingData, Q, A] {

  @transient lazy val logger = Logger[this.type]
  @transient lazy val batchView = new LBatchView(dsp.appId,
    dsp.startTime, dsp.untilTime)

  override
  def read(): Seq[(DP, TrainingData, Seq[(Q, A)])] = {
    if (dsp.slidingEval.isEmpty) {
      val (uid2ui, users) = extractUsers(dsp.untilTime)
      val (iid2ii, items) = extractItems(dsp.untilTime)
      val actions = extractActions(uid2ui, iid2ii, dsp.startTime, dsp.untilTime)

      val trainingData = new TrainingData(
        users = HashMap[Int, UserTD]() ++ users,
        items = HashMap[Int, ItemTD]() ++ items,
        u2iActions = actions.toList)

      return Seq((null.asInstanceOf[DP], trainingData, Seq[(Q, A)]()))
    } else {
      val evalParams = dsp.slidingEval.get
      val evalDuration = evalParams.evalDuration
      val firstTrainUntil = evalParams.firstTrainingUntilTime

      return (0 until evalParams.evalCount).map { idx => {
        // Use [dsp.startTime, firstTrain + idx * duration) as training
        val trainUntil = firstTrainUntil.plus(idx * evalDuration.getMillis)
        val evalStart = trainUntil
        val evalUntil = evalStart.plus(evalDuration)

        println(s"Eval $idx " +
            s"train: [, $trainUntil) eval: [$evalStart, $evalUntil)")
     
        println("a")
        val (uid2ui, users) = extractUsers(Some(trainUntil))
        println("b")
        val (iid2ii, items) = extractItems(Some(trainUntil))
        println("c")
        val trainActions = extractActions(
          uid2ui, 
          iid2ii, 
          startTimeOpt = dsp.startTime, 
          untilTimeOpt = Some(trainUntil))

        println("d")
        val trainingData = new TrainingData(
          users = HashMap[Int, UserTD]() ++ users,
          items = HashMap[Int, ItemTD]() ++ items,
          u2iActions = trainActions.toList)

        // Use [firstTrain + idx * duration, firstTraing + (idx+1) * duration)
        // as testing
        println("e")
        val evalActions = extractActions(
          uid2ui, 
          iid2ii, 
          startTimeOpt = Some(evalStart),
          untilTimeOpt = Some(evalUntil))
          
        println("f")
        val (dp, qaSeq) = generateQueryActualSeq(
          users, items, evalActions)

        println("g")
        (dp, trainingData, qaSeq)
      }}
    }
  }

  // sub-classes should override this method.
  def generateQueryActualSeq(
    users: Map[Int, UserTD],
    items: Map[Int, ItemTD],
    actions: Seq[U2IActionTD]): (DP, Seq[(Q, A)]) = {
    // first return value is a fake data param to make compiler happy
    (null.asInstanceOf[DP], Seq[(Q, A)]())
  }

  def extractUsers(untilTimeOpt: Option[DateTime] = None)
  : (Map[String, Int], Map[Int, UserTD]) = {
    val attributeNames = dsp.attributeNames

    val usersMap: Map[Int, String] = batchView
    .aggregateProperties(
      entityType = attributeNames.user,
      untilTimeOpt = untilTimeOpt)
    .zipWithIndex
    .mapValues(_ + 1)  // make index 1-based
    .map(_.swap)
    .mapValues(_._1)  // value._2 is a DataMap, unused by user.

    (usersMap.map(_.swap), 
      usersMap.mapValues(entityId => new UserTD(uid=entityId)))
  }

  def extractItems(untilTimeOpt: Option[DateTime] = None)
  : (Map[String, Int], Map[Int, ItemTD]) = {
    val attributeNames = dsp.attributeNames
    val itemsMap: Map[String, ItemTD] = batchView
      .aggregateProperties(
        entityType = attributeNames.item,
        untilTimeOpt = untilTimeOpt)
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
      }
      .filter { case (id, (itemTD)) =>
        // TODO. Traverse itemTD.itypes to avoid a toSet function. Looking up
        // dsp.itypes is constant time.
        dsp.itypes
        .map{ t =>
          !(itemTD.itypes.toSet.intersect(t).isEmpty)
        }.getOrElse(true)
      }

    val indexMap: Map[Int, (String, ItemTD)] = 
      itemsMap.zipWithIndex.mapValues(_ + 1).map(_.swap)

    (indexMap.mapValues(_._1).map(_.swap), indexMap.mapValues(_._2))
  }

  def extractActions(
    uid2ui: Map[String, Int],
    iid2ii: Map[String, Int],
    startTimeOpt: Option[DateTime] = None,
    untilTimeOpt: Option[DateTime] = None
  ): Seq[U2IActionTD] = {
    val attributeNames = dsp.attributeNames

    batchView.events
    .filter { e => (true
      && attributeNames.u2iActions.contains(e.event)
      && dsp.actions.contains(e.event)
      // TODO. Add a flag to allow unseen users
      && uid2ui.contains(e.entityId)
      // TODO. Add a flag to allow unseen items
      && e.targetEntityId.map(iid2ii.contains(_)).getOrElse(true)
    )}
    .map { e =>
      require(
        (e.targetEntityId != None),
        s"u2i Event: ${e} cannot have targetEntityId empty.")
      try {
        new U2IActionTD(
          //uindex = usersMap(e.entityId)._2,
          //iindex = itemsMap(e.targetEntityId.get)._2,
          uindex = uid2ui(e.entityId),
          iindex = iid2ii(e.targetEntityId.get),
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
  }

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

    return (
      usersMap.values.map(_.swap).toMap,
      itemsMap.values.map(_.swap).toMap,
      u2iActions)
  }
}
