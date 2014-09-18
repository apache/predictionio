package io.prediction.engines.itemrec

import io.prediction.controller.EmptyDataParams
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
  val attributeNames: base.AttributeNames
) extends base.AbstractEventsDataSourceParams

class EventsDataSource(dsp: EventsDataSourceParams)
  extends base.EventsDataSource[EmptyDataParams, Query, Actual](dsp)
