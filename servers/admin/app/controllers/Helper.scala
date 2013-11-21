package controllers

import play.api.Logger

import Application.{users, apps, engines, engineInfos, algos, algoInfos}
import Application.{offlineEvalMetricInfos, offlineEvals, offlineEvalMetrics, offlineEvalResults}
import Application.{offlineEvalSplitters, offlineTunes, paramGens}
import Application.{appDataUsers, appDataItems, appDataU2IActions}
import Application.{trainingSetUsers, trainingSetItems, trainingSetU2IActions}
import Application.{validationSetUsers, validationSetItems, validationSetU2IActions}
import Application.{testSetUsers, testSetItems, testSetU2IActions}
import Application.{itemRecScores, itemSimScores}
import Application.{trainingItemRecScores, trainingItemSimScores}
import Application.settingsSchedulerUrl

import io.prediction.commons.settings.{OfflineEval, OfflineTune, Algo}

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS
import play.api.mvc.Controller
import play.api.libs.json.{Json}
import play.api.http

/** helper functions */
object Helper extends Controller {

  /** check if this simeval is pending */
  def isPendingSimEval(eval: OfflineEval): Boolean = (eval.createtime != None) && (eval.endtime == None)
  
  /** Check if this offline tune is pending */
  def isPendingOfflineTune(tune: OfflineTune): Boolean = (tune.createtime != None) && (tune.endtime == None)

  /** Check if algo is available */
  def isAvailableAlgo(algo: Algo): Boolean = !((algo.status == "deployed") || (algo.status == "simeval"))

  def isSimEvalAlgo(algo: Algo): Boolean = (algo.status == "simeval")

  /** Return sim evals of this engine */
  def getSimEvalsByEngineid(engineid: Int): Iterator[OfflineEval] = offlineEvals.getByEngineid(engineid).filter( _.tuneid == None )

  /** Delete appdata DB of this appid
    */
  def deleteAppData(appid: Int) = {
    Logger.info("Delete appdata for app ID "+appid)
    appDataUsers.deleteByAppid(appid)
    appDataItems.deleteByAppid(appid)
    appDataU2IActions.deleteByAppid(appid)
  }

  /** Delete training set data of this evalid
    */
  def deleteTrainingSetData(evalid: Int) = {
    Logger.info("Delete training set for offline eval ID "+evalid)
    trainingSetUsers.deleteByAppid(evalid)
    trainingSetItems.deleteByAppid(evalid)
    trainingSetU2IActions.deleteByAppid(evalid)
  }

  /**
    * Delete validation set data of this evalid
    */
  def deleteValidationSetData(evalid: Int) = {
    Logger.info("Delete validation set for offline eval ID "+evalid)
    validationSetUsers.deleteByAppid(evalid)
    validationSetItems.deleteByAppid(evalid)
    validationSetU2IActions.deleteByAppid(evalid)
  }

  /**
    * Delete test set data of this evalid
    */
  def deleteTestSetData(evalid: Int) = {
    Logger.info("Delete test set for offline eval ID "+evalid)
    testSetUsers.deleteByAppid(evalid)
    testSetItems.deleteByAppid(evalid)
    testSetU2IActions.deleteByAppid(evalid)
  }

  /**
    * Delete modeldata of this algoid
    */
  def deleteModelData(algoid: Int) = {
    val algoOpt = algos.get(algoid)
    algoOpt map { algo =>
      algoInfos.get(algo.infoid) map { algoInfo =>
        if (algo.status == "simeval") {
          Logger.info("Delete training model data for algo ID " + algoid)
          algoInfo.engineinfoid match {
            case "itemrec" => trainingItemRecScores.deleteByAlgoid(algoid)
            case "itemsim" => trainingItemSimScores.deleteByAlgoid(algoid)
            case _ => throw new RuntimeException("Try to delete algo of unsupported engine type: " + algoInfo.engineinfoid)
          }
        } else {
          Logger.info("Delete model data for algo ID " + algoid)
          algoInfo.engineinfoid match {
            case "itemrec" => itemRecScores.deleteByAlgoid(algoid)
            case "itemsim" => itemSimScores.deleteByAlgoid(algoid)
            case _ => throw new RuntimeException("Try to delete algo of unsupported engine type: " + algoInfo.engineinfoid)
          }
        }
      } getOrElse { throw new RuntimeException("Try to delete algo of non-existing algotype: " + algo.infoid) }
    } getOrElse { throw new RuntimeException("Try to delete non-existing algo: " + algoid) }
  }


  /** Delete this app and the assoicated engines and appdata
    * @param appid the appid
    * @param userid the userid
    * @param keepSettings keepSettings flag. If this is true, keep all settings record (ie. only delete the appdata, modeldata)
    */
  def deleteApp(appid: Int, userid: Int, keepSettings: Boolean) = {

    val appEngines = engines.getByAppid(appid)

    appEngines foreach { eng =>
      deleteEngine(eng.id, appid, keepSettings)
    }

    deleteAppData(appid)

    if (!keepSettings) {
      Logger.info("Delete app ID " + appid)
      apps.deleteByIdAndUserid(appid, userid)
    }
  }

  /** Delete engine and the associated algos and simevals.
   */
  def deleteEngine(engineid: Int, appid: Int, keepSettings: Boolean) = {

    // delete non-sim eval algos, "simeval" algo is deleted when delete sim eval later
    val engineAlgos = algos.getByEngineid(engineid).filter( !isSimEvalAlgo(_) )

    engineAlgos foreach { algo =>
      deleteAlgo(algo.id, keepSettings)
    }

    val simEvals = getSimEvalsByEngineid(engineid)

    simEvals foreach { eval =>
      deleteOfflineEval(eval.id, keepSettings)
    }

    if (!keepSettings) {
      Logger.info("Delete engine ID " + engineid)
      engines.deleteByIdAndAppid(engineid, appid)
    }
  }

  /** Delete non-simeval algo and associated modeldata, offlineTune
    */
  def deleteAlgo(algoid: Int, keepSettings: Boolean) = {
    deleteModelData(algoid)

    algos.get(algoid) map { algo =>
      algo.offlinetuneid map { tuneid =>
        deleteOfflineTune(tuneid, keepSettings)
      }
    }

    if (!keepSettings) {
      Logger.info("Delete algo ID " + algoid)
      algos.delete(algoid)
    }
  }
  
  /** Delete "simeval" algo and assoicated modeldata */
  def deleteSimEvalAlgo(algoid: Int, keepSettings: Boolean) = {
    deleteModelData(algoid)

    if (!keepSettings) {
      Logger.info("Delete simeval algo ID " + algoid)
      algos.delete(algoid)
    }
  }

  /** Delete offline tune and associated trainig/validation/test set data, evaluated algos, metrics, eval results, and splitters
   */
  def deleteOfflineEval(evalid: Int, keepSettings: Boolean) = {

    deleteTrainingSetData(evalid)
    deleteValidationSetData(evalid)
    deleteTestSetData(evalid)

    val evalAlgos = algos.getByOfflineEvalid(evalid)

    evalAlgos foreach { algo =>
      deleteSimEvalAlgo(algo.id, keepSettings)
    }

    if (!keepSettings) {
      val evalMetrics = offlineEvalMetrics.getByEvalid(evalid)

      evalMetrics foreach { metric =>
        Logger.info("Delete metric ID "+metric.id)
        offlineEvalMetrics.delete(metric.id)
      }

      Logger.info("Delete offline eval results of offline eval ID "+evalid)
      offlineEvalResults.deleteByEvalid(evalid)

      val evalSplitters = offlineEvalSplitters.getByEvalid(evalid)
      evalSplitters foreach { splitter =>
        Logger.info("Delete Offline Eval Splitter ID "+splitter.id)
        offlineEvalSplitters.delete(splitter.id)
      }
    }

    if (!keepSettings) {
      Logger.info("Delete offline eval ID " + evalid)
      offlineEvals.delete(evalid)
    }

  }

  /** Delete offline tune and associated param gens and offline evals
   */
  def deleteOfflineTune(tuneid: Int, keepSettings: Boolean) = {

    // delete paramGen
    if (!keepSettings) {
      val tuneParamGens = paramGens.getByTuneid(tuneid)
      tuneParamGens foreach { gen =>
        Logger.info("Delete ParamGen ID "+gen.id)
        paramGens.delete(gen.id)
      }
    }

    // delete OfflineEval
    val tuneOfflineEvals = offlineEvals.getByTuneid(tuneid)

    tuneOfflineEvals foreach { eval =>
      deleteOfflineEval(eval.id, keepSettings)
    }
    
    if (!keepSettings) {
      Logger.info("Delete offline tune ID " + tuneid)
      offlineTunes.delete(tuneid)
    }
  }

  /** Request scheduler to stop and delete sim eval
    * @return Future[SimpleResult]
    */
  def stopAndDeleteSimEvalScheduler(appid: Int, engineid: Int, evalid: Int) = {

    /** Stop any possible running jobs */
    val stop = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/offlineevals/${evalid}/stop").get()
    /** Clean up intermediate data files */
    val delete = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/offlineevals/${evalid}/delete").get()
    /** Synchronize on both scheduler actions */
    val remove = concurrent.Future.reduce(Seq(stop, delete)) { (a, b) => 
      if (a.status != http.Status.OK) // keep the 1st error
        a
      else 
        b
    }

    /** Handle any error that might occur within the Future */
    val complete = remove map { r =>
      if (r.status == http.Status.OK)
        Ok(Json.obj("message" -> s"Offline evaluation ID ${evalid} has been deleted"))
      else
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String] ))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete simulated evaluation. Please check if the scheduler server is running properly. " + e.getMessage())))
    }

    complete
  }

  /** Request scheduler to stop and delete offline Tune
    * @return Future[SimpleResult]
    */
  def stopAndDeleteOfflineTuneScheduler(appid: Int, engineid: Int, tuneid: Int) = {

    val stop = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/offlinetunes/${tuneid}/stop").get()

    val deletes = offlineEvals.getByTuneid(tuneid) map { eval =>
      WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/offlineevals/${eval.id}/delete").get()
    }

    val remove = concurrent.Future.reduce(Seq(stop) ++ deletes) { (a, b) => 
      if (a.status != http.Status.OK) // keep the 1st error
        a
      else 
        b
    }

    val complete = remove map { r =>
      if (r.status == http.Status.OK)
        Ok(Json.obj("message" -> s"Offline Tune ID ${tuneid} has been deleted"))
      else
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String] ))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete autotuning algorithm. Please check if the scheduler server is running properly. " + e.getMessage())))
    }
    
    complete

}

  /** Request scheduler to delete algo file 
    * @return Future[SimpleResult]
    */
  def deleteAlgoScheduler(appid: Int, engineid: Int, id: Int) = {
    val delete = WS.url(settingsSchedulerUrl+"/apps/"+appid+"/engines/"+engineid+"/algos/"+id+"/delete").get()

    delete map { r =>
      if (r.status == http.Status.OK)
        Ok
      else
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String] ))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete algorithm. Please check if the scheduler server is running properly. " + e.getMessage())))
    }
  }

  /** Request scheduler to delete engine file
    * @return Future[SimpleResult]
    */
  def deleteEngineScheduler(appid: Int, engineid: Int) = {
    val delete = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/delete").get()

    delete map { r =>
      if (r.status == http.Status.OK)
        Ok
      else
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String] ))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete engine. Please check if the scheduler server is running properly. " + e.getMessage())))
    }
  }

  /** Request scheduler to delete app file
    * @return Future[SimpleResult]
    */
  def deleteAppScheduler(appid: Int) = {
    val delete = WS.url(settingsSchedulerUrl+"/apps/"+appid+"/delete").get()

    delete map { r =>
      if (r.status == http.Status.OK)
        Ok
      else
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String] ))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete app. Please check if the scheduler server is running properly. " + e.getMessage())))
    }
  }

}