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

import io.prediction.controller.PDataSource
import io.prediction.data.view.PBatchView
import io.prediction.data.view.ViewPredicates

import org.joda.time.DateTime
import org.joda.time.Duration

import scala.reflect.ClassTag

import grizzled.slf4j.Logger

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

class PEventsDataSource[DP: ClassTag, Q, A](
  dsp: AbstractEventsDataSourceParams)
  extends PDataSource[DP, PTrainingData, Q, A] {

  @transient lazy val logger = Logger[this.type]

  override
  def read(sc: SparkContext): Seq[(DP, PTrainingData, RDD[(Q, A)])] = {

    val batchView = new PBatchView(
      appId = dsp.appId,
      startTime = dsp.startTime,
      untilTime = dsp.untilTime,
      sc = sc)

    if (dsp.slidingEval.isEmpty) {
      val (uid2ui, users) = extractUsers(batchView, dsp.untilTime)
      val (iid2ii, items) = extractItems(batchView, dsp.untilTime)
      val actions = extractActions(batchView, uid2ui, iid2ii,
        dsp.startTime, dsp.untilTime)

      val trainingData = new PTrainingData(
        users = users,
        items = items,
        u2iActions = actions)

      return Seq((null.asInstanceOf[DP], trainingData,
        sc.parallelize(Seq[(Q, A)]())))

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

        val (uid2ui, users) = extractUsers(batchView, Some(trainUntil))
        val (iid2ii, items) = extractItems(batchView, Some(trainUntil))
        val trainActions = extractActions(
          batchView,
          uid2ui,
          iid2ii,
          startTimeOpt = dsp.startTime,
          untilTimeOpt = Some(trainUntil))

        val trainingData = new PTrainingData(
          users = users,
          items = items,
          u2iActions = trainActions)

        // Use [firstTrain + idx * duration, firstTraing + (idx+1) * duration)
        // as testing
        val evalActions = extractActions(
          batchView,
          uid2ui,
          iid2ii,
          startTimeOpt = Some(evalStart),
          untilTimeOpt = Some(evalUntil))

        val (dp, qaSeq) = generateQueryActualSeq(
          users, items, evalActions, trainUntil, evalStart, evalUntil, sc)

        (dp, trainingData, qaSeq)
      }}
    }
  }

  // sub-classes should override this method.
  def generateQueryActualSeq(
    users: RDD[(Int, UserTD)],
    items: RDD[(Int, ItemTD)],
    actions: RDD[U2IActionTD],
    trainUntil: DateTime,
    evalStart: DateTime,
    evalUntil: DateTime,
    sc: SparkContext): (DP, RDD[(Q, A)]) = {
    // first return value is a fake data param to make compiler happy
    (null.asInstanceOf[DP], sc.parallelize(Seq[(Q, A)]()))
  }

  def extractUsers(batchView: PBatchView,
    untilTimeOpt: Option[DateTime] = None)
  : (RDD[(String, Int)], RDD[(Int, UserTD)]) = {
    val attributeNames = dsp.attributeNames

    val usersMap: RDD[((String, UserTD), Int)] = batchView
    .aggregateProperties(
      entityType = attributeNames.user,
      untilTimeOpt = untilTimeOpt)
    .map { case (entityId, dataMap) =>
      (entityId, new UserTD(uid = entityId))
    }
    .zipWithUniqueId // theis Long id may exist gaps but no need spark job
    // TODO: may need to change local EventDataSource to use Long.
    // Force to Int now so can re-use same userTD, itemTD, and ratingTD
    .mapValues( _.toInt )

    (usersMap.map{ case ((uid, uTD), idx) => (uid, idx) },
      usersMap.map{ case ((uid, uTD), idx) => (idx, uTD) })
  }

  def extractItems(batchView: PBatchView,
    untilTimeOpt: Option[DateTime] = None)
  : (RDD[(String, Int)], RDD[(Int, ItemTD)]) = {
    val attributeNames = dsp.attributeNames
    val itemsMap: RDD[((String, ItemTD), Int)] = batchView
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
      .zipWithUniqueId // the Long id may exist gaps but no need spark job
      // TODO: may need to change local EventDataSource to use Long.
      // Force to Int now so can re-use same userTD, itemTD, and ratingTD
      .mapValues( _.toInt )

    (itemsMap.map{ case ((iid, iTD), idx) => (iid, idx) },
      itemsMap.map{ case ((iid, iTD), idx) => (idx, iTD) })
  }

  def extractActions(batchView: PBatchView,
    uid2ui: RDD[(String, Int)],
    iid2ii: RDD[(String, Int)],
    startTimeOpt: Option[DateTime] = None,
    untilTimeOpt: Option[DateTime] = None
  ): RDD[U2IActionTD] = {
    val attributeNames = dsp.attributeNames

    batchView
    .events
    .filter( e => (true
      && ViewPredicates.getStartTimePredicate(startTimeOpt)(e)
      && ViewPredicates.getUntilTimePredicate(untilTimeOpt)(e)
      && attributeNames.u2iActions.contains(e.event)
      && dsp.actions.contains(e.event)
    ))
    // TODO: can use broadcast variable if uid2ui and iid2ui is small
    // so no need to join to avoid shuffle
    .map( e => (e.entityId, e) )
    .join(uid2ui) // (entityID, (e, ui))
    .map{ case (eid, (e, ui)) =>
      require(
        (e.targetEntityId != None),
        s"u2i Event: ${e} cannot have targetEntityId empty.")
      (e.targetEntityId.get, (e, ui))
    }
    .join(iid2ii) // (targetEntityId, ((e, ui), ii))
    .map{ case (teid, ((e, ui), ii)) =>
      try {
        new U2IActionTD(
          uindex = ui,
          iindex = ii,
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

}
