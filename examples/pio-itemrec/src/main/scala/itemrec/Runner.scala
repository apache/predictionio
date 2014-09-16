package io.prediction.engines.itemrec

import io.prediction.engines.itemrank.EventsDataSource
import io.prediction.engines.itemrank.EventsDataSourceParams
import io.prediction.engines.itemrank.AttributeNames
import io.prediction.engines.itemrank.PreparatorParams

import io.prediction.controller._

//import io.prediction.engines.itemrec.Model

object Runner {

  def main(args: Array[String]) {
    val dsp = EventsDataSourceParams(
      appId = 4,
      itypes = None,
      actions = Set("view", "like", "dislike", "conversion", "rate"),
      startTime = None,
      untilTime = None,
      attributeNames = AttributeNames(
        user = "pio_user",
        item = "pio_item",
        u2iActions = Set("view", "like", "dislike", "conversion", "rate"),
        itypes = "pio_itypes",
        starttime = "pio_starttime",
        endtime = "pio_endtime",
        inactive = "pio_inactive",
        rating = "pio_rate"
      )
    )     

    val pp = new PreparatorParams(
      actions = Map(
        "view" -> Some(3),
        "like" -> Some(5),
        "conversion" -> Some(4),
        "rate" -> None
      ),
      conflict = "latest"
    )

    Workflow.run(
      dataSourceClassOpt = Some(classOf[EventsDataSource]),
      dataSourceParams = dsp,
      preparatorClassOpt = Some(classOf[NewItemRecPreparator]),
      preparatorParams = pp,
      params = WorkflowParams(
        verbose = 3,
        batch = "PIO: ItemRec"))

    /*
    val engine = ItemRecEngine()
    val engineParams = new EngineParams(
      dataSourceParams = dsp
    )

    Workflow.runEngine(
      params = WorkflowParams(
        batch = "PIO: ItemRec",
        verbose = 3),
      engine = engine,
      engineParams = engineParams)
    */
  }
}
