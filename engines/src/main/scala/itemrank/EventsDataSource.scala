package io.prediction.engines.itemrank

import io.prediction.controller.LDataSource
import io.prediction.controller.Params
import io.prediction.controller.EmptyDataParams
import io.prediction.data.view.LBatchView

import org.joda.time.DateTime

case class EventsDataSourceParams(
  val appId: Int,
  // default None to include all itypes
  val itypes: Option[Set[String]] = None, // train items with these itypes
  // actions for training
  val actions: Set[String],
  val startTime: Option[DateTime] = None, // event starttime
  val untilTime: Option[DateTime] = None, // event untiltime
  val attributeNames: AttributeNames
) extends Params


class EventsDataSource(dsp: EventsDataSourceParams)
  extends LDataSource[EventsDataSourceParams,
    DataParams, TrainingData, Query, Actual] {

  @transient lazy val batchView = new LBatchView(dsp.appId,
    dsp.startTime, dsp.untilTime)

  override
  def readTraining(): TrainingData = {

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
        val itemTD = new ItemTD(
          iid = entityId,
          itypes = dataMap.get[List[String]](attributeNames.itypes),
          starttime = dataMap.getOpt[DateTime](attributeNames.starttime)
            .map(_.getMillis),
          endtime = dataMap.getOpt[DateTime](attributeNames.endtime)
            .map(_.getMillis),
          inactive = dataMap.getOpt[Boolean](attributeNames.inactive)
            .getOrElse(false)
        )
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
        new U2IActionTD(
          uindex = usersMap(e.entityId)._2,
          iindex = itemsMap(e.targetEntityId.get)._2,
          action = e.event,
          v = e.properties.getOpt[Int](attributeNames.rating),
          t = e.eventTime.getMillis
        )
      }

    new TrainingData(
      users = usersMap.map { case (k, (v1, v2)) => (v2, v1) },
      items = itemsMap.map { case (k, (v1, v2)) => (v2, v1) },
      u2iActions = u2iActions
    )
  }
}
