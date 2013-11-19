package controllers

import play.api._
import play.api.mvc._

import com.github.nscala_time.time.Imports._

import io.prediction.commons.settings.{OfflineEval, Algo, OfflineEvalSplitter, OfflineEvalMetric}
import controllers.Application.{offlineEvals, algos, engines, offlineEvalSplitters, offlineEvalMetrics}

object SimEval extends Controller {

  /** common function to create Offline Eval
   * @param tuneid specify offline tune id if this Offine Eval is for auto tune
   */
  def createSimEval(engineId: Int, listOfAlgos: List[Algo], metricTypes: List[String], metricSettings: List[String],
    splitTrain: Int, splitValidation: Int, splitTest: Int, splitMethod: String, evalIteration: Int, tuneid: Option[Int]) = {

    // insert offlineeval record without create time
    val newOfflineEval = OfflineEval(
      id = -1,
      engineid = engineId,
      name = "",
      iterations = evalIteration,
      tuneid = tuneid,
      createtime = None, // NOTE: no createtime yet
      starttime = None,
      endtime = None
    )

    val evalid = offlineEvals.insert(newOfflineEval)
    Logger.info("Create offline eval ID " + evalid)

    // duplicate algo with evalid
    for ( algo <- listOfAlgos ) {
      // duplicate algo for sim eval
      val algoid = algos.insert(algo.copy(
        id = -1,
        offlineevalid = Option(evalid),
        status = "simeval"
      ))
      Logger.info("Create sim eval algo ID " + algoid)
    }

    val engine = engines.get(engineId).get
    val metricInfoId = engine.infoid match {
      case "itemrec" => "map_k"
      case "itemsim" => "ismap_k"
      case _ => ""
    }

    // create metric record with evalid
    for ((metricType, metricSetting) <- (metricTypes zip metricSettings)) {
      val metricId = offlineEvalMetrics.insert(OfflineEvalMetric(
        id = -1,
        infoid = metricInfoId,
        evalid = evalid,
        params = Map("kParam" -> metricSetting) // TODO: hardcode param index name for now, should depend on metrictype
      ))
      Logger.info("Create metric ID " + metricId)
    }

    // create splitter record
    val splitterId = offlineEvalSplitters.insert(OfflineEvalSplitter(
      id = -1,
      evalid = evalid,
      name = ("sim-eval-" + evalid + "-splitter"), // auto generate name now
      infoid = "trainingtestsplit", // TODO: support different splitter
      settings = Map(
        "trainingPercent" -> (splitTrain.toDouble/100),
        "validationPercent" -> (splitValidation.toDouble/100), // no validatoin set for sim eval
        "testPercent" -> (splitTest.toDouble/100),
        "timeorder" -> (splitMethod != "random")
        )
    ))
    Logger.info("Create offline eval splitter ID " + splitterId)

    // after all algo and metric info is stored.
    // update offlineeval record with createtime, so scheduler can know it's ready to be picked up
    offlineEvals.update(newOfflineEval.copy(
      id = evalid,
      name = ("sim-eval-" + evalid), // TODO: auto generate name now
      createtime = Option(DateTime.now)
    ))
  }
}
