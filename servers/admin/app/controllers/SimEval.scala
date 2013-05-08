package controllers

import play.api._
import play.api.mvc._

import com.github.nscala_time.time.Imports._

import io.prediction.commons.settings.{OfflineEval, Algo, OfflineEvalSplitter, OfflineEvalMetric}
import controllers.Application.{offlineEvals, algos, offlineEvalSplitters, offlineEvalMetrics}

object SimEval extends Controller {

//appId, engineId, algoIds, metricTypes, metricSettings, splitTrain, splitTest, splitMethod, evalIteration)

  def createSimEval(engineId: Int, algoIds: List[Int], metricTypes: List[String], metricSettings: List[String],
    splitTrain: Int, splitValidation: Int, splitTest: Int, splitMethod: String, evalIteration: Int, autoTune: Boolean): Boolean = {

    // insert offlineeval record without create time
    val newOfflineEval = OfflineEval(
      id = -1,
      engineid = engineId,
      name = "",
      iterations = evalIteration,
      trainingsize = 8, // TODO: remove
      testsize = 2, // TODO: remove
      timeorder = false, // TODO: remove
      tuneid = None, // TODO: create tune record first and put id here
      createtime = None, // NOTE: no createtime yet
      starttime = None,
      endtime = None
    )

    val evalid = offlineEvals.insert(newOfflineEval)

    val optAlgos: List[Option[Algo]] = algoIds map {algoId => algos.get(algoId)}

    if (!optAlgos.contains(None)) {

      // duplicate algo with evalid
      for ( optAlgo <- optAlgos ) {
        if (autoTune) {
          // if auto tune, update algo with offlineevalid only.
          algos.update(optAlgo.get.copy(
            offlineevalid = Option(evalid),
            status = "tuning"
          ))
        } else {
          // duplicate algo for sim eval
          algos.insert(optAlgo.get.copy(
            id = -1,
            offlineevalid = Option(evalid),
            status = "simeval"
          ))
        }
      }

      // create metric record with evalid
      for ((metricType, metricSetting) <- (metricTypes zip metricSettings)) {
        val metricId = offlineEvalMetrics.insert(OfflineEvalMetric(
          id = -1,
          infoid = "map_k",
          evalid = evalid,
          params = Map("kParam" -> metricSetting) // TODO: hardcode param index name for now, should depend on metrictype
        ))
      }

      // create splitter record
      offlineEvalSplitters.insert(OfflineEvalSplitter(
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

      // after all algo and metric info is stored.
      // update offlineeval record with createtime, so scheduler can know it's ready to be picked up
      offlineEvals.update(newOfflineEval.copy(
        id = evalid,
        name = ("sim-eval-" + evalid), // TODO: auto generate name now
        createtime = Option(DateTime.now)
      ))

      true // success status

    } else {

      // there is error in getting algos, delete the offline eval record inserted.
      offlineEvals.delete(evalid)

      false // fail status
    }
  }

}