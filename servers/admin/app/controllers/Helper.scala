package controllers

import play.api.Logger

import Application.{ users, apps, engines, engineInfos, algos, algoInfos }
import Application.{ offlineEvalMetricInfos, offlineEvals, offlineEvalMetrics, offlineEvalResults }
import Application.{ offlineEvalSplitters, offlineTunes, paramGens }
import Application.{ appDataUsers, appDataItems, appDataU2IActions }
import Application.{ trainingSetUsers, trainingSetItems, trainingSetU2IActions }
import Application.{ validationSetUsers, validationSetItems, validationSetU2IActions }
import Application.{ testSetUsers, testSetItems, testSetU2IActions }
import Application.{ itemRecScores, itemSimScores }
import Application.{ trainingItemRecScores, trainingItemSimScores }
import Application.settingsSchedulerUrl

import io.prediction.commons.settings.{ Algo, Engine }
import io.prediction.commons.settings.{ OfflineEval, OfflineTune, OfflineEvalMetric, OfflineEvalSplitter }
import io.prediction.commons.settings.{ AlgoInfo, OfflineEvalMetricInfo, OfflineEvalSplitterInfo }

import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS
import play.api.mvc.Controller
import play.api.libs.json.{ JsNull, JsArray, Json, JsValue, Writes, JsObject }
import play.api.http

import com.github.nscala_time.time.Imports._

/** helper functions */
object Helper extends Controller {

  /** Check if the offlineEval is simeval */
  def isSimEval(eval: OfflineEval): Boolean = (eval.tuneid == None)

  /** check if this simeval is pending */
  def isPendingSimEval(eval: OfflineEval): Boolean = isSimEval(eval) && (eval.createtime != None) && (eval.endtime == None)

  /** Check if this offline tune is pending */
  def isPendingOfflineTune(tune: OfflineTune): Boolean = (tune.createtime != None) && (tune.endtime == None)

  /** Check if algo is available */
  def isAvailableAlgo(algo: Algo): Boolean = !((algo.status == "deployed") || (algo.status == "simeval"))

  def isSimEvalAlgo(algo: Algo): Boolean = (algo.status == "simeval")

  /** Return sim evals of this engine */
  def getSimEvalsByEngineid(engineid: Int): Iterator[OfflineEval] = offlineEvals.getByEngineid(engineid).filter(isSimEval(_))

  def getSimEvalStatus(eval: OfflineEval): String = {
    val status = (eval.createtime, eval.starttime, eval.endtime) match {
      case (Some(x), Some(y), Some(z)) => "completed"
      case (Some(x), Some(y), None) => "running"
      case (Some(x), None, Some(z)) => "internal error"
      case (Some(x), None, None) => "pending"
      case (None, _, _) => "canceled"
    }
    status
  }

  def getOfflineTuneStatus(tune: OfflineTune): String = {
    val status: String = (tune.createtime, tune.starttime, tune.endtime) match {
      case (Some(x), Some(y), Some(z)) => "completed"
      case (Some(x), Some(y), None) => "running"
      case (Some(x), None, Some(z)) => "internal error"
      case (Some(x), None, None) => "pending"
      case (None, _, _) => "canceled"
    }
    status
  }
  /**
   * Convert algo data to JsObject
   *   {
   *     "id" : <algo id>,
   *     "algoname" : <algo name>,
   *     "appid" : <app id>,
   *     "engineid" : <engine id>,
   *     "algoinfoid" : <algo info id>,
   *     "algoinfoname" : <algo info name>,
   *     "status" : <algo status>,
   *     "createdtime" : <algo creation time>,
   *     "updatedtime" : <algo last updated time>
   *   }
   * @note status: ready, deployed, tuning, tuned, simeval
   * @param algo the algo
   * @param appid the App ID
   * @param algoinfo AlgoInfo
   */
  def algoToJson(algo: Algo, appid: Int, algoinfoOpt: Option[AlgoInfo], withParam: Boolean = false): JsObject = {
    val infoname = algoinfoOpt.map(_.name).getOrElse[String](s"algoinfo ${algo.infoid} not found")

    if (withParam)
      Json.obj(
        "id" -> algo.id,
        "algoname" -> algo.name,
        "appid" -> appid,
        "engineid" -> algo.engineid,
        "algoinfoid" -> algo.infoid,
        "algoinfoname" -> infoname,
        "settingsstring" -> algoParamToString(algo, algoinfoOpt)
      )
    else
      Json.obj(
        "id" -> algo.id,
        "algoname" -> algo.name,
        "appid" -> appid,
        "engineid" -> algo.engineid,
        "algoinfoid" -> algo.infoid,
        "algoinfoname" -> infoname,
        "status" -> algo.status,
        "createdtime" -> dateTimeToString(algo.createtime),
        "updatedtime" -> dateTimeToString(algo.updatetime)
      )
  }

  def offlineEvalMetricToJson(metric: OfflineEvalMetric, metricinfoOpt: Option[OfflineEvalMetricInfo], withParam: Boolean = false): JsObject = {
    val infoname = metricinfoOpt.map(_.name).getOrElse[String](s"offlineevalmetricinfo ${metric.infoid} not found")
    if (withParam)
      Json.obj(
        "id" -> metric.id,
        "metricsinfoid" -> metric.infoid,
        "metricsname" -> infoname,
        "settingsstring" -> offlineEvalMetricParamToString(metric, metricinfoOpt)
      )
    else
      Json.obj(
        "id" -> metric.id,
        "metricsinfoid" -> metric.infoid,
        "metricsname" -> infoname
      )
  }

  def algoParamToString(algo: Algo, algoinfoOpt: Option[AlgoInfo]): String = {
    algoinfoOpt.map { algoInfo =>
      algoInfo.paramorder.map { paramid =>
        algoInfo.params(paramid).name + " = " +
          algo.params.getOrElse(paramid, algoInfo.params(paramid).defaultvalue)
      }.mkString(", ")
    }.getOrElse(s"algoinfo ${algo.infoid} not found")
  }

  def offlineEvalMetricParamToString(metric: OfflineEvalMetric, metricinfoOpt: Option[OfflineEvalMetricInfo]): String = {
    metricinfoOpt.map { metricInfo =>
      metricInfo.paramorder.map { paramid =>
        metricInfo.params(paramid).name + " = " +
          metric.params.getOrElse(paramid, metricInfo.params(paramid).defaultvalue)
      }.mkString(", ")
    }.getOrElse(s"offlineevalmetricinfo ${metric.infoid} not found")
  }

  def offlineEvalSplitterParamToString(splitter: OfflineEvalSplitter, splitterinfoOpt: Option[OfflineEvalSplitterInfo]): String = {
    splitterinfoOpt.map { splitterInfo =>
      splitterInfo.paramorder.map { paramid =>
        val param = splitterInfo.params(paramid)

        val value = splitter.settings.getOrElse(paramid, splitterInfo.params(paramid).defaultvalue)
        val displayValue = param.ui.uitype match {
          // if selection, display the name of the selection instead of value.
          case "selection" => {
            val names: Map[String, String] = param.ui.selections.map(_.map(s => (s.value, s.name)).toMap[String, String]).getOrElse(Map[String, String]())
            names.getOrElse(value.toString, value.toString)
          }
          case _ => value
        }
        splitterInfo.params(paramid).name + ": " + displayValue
      }.mkString(", ")
    }.getOrElse(s"OfflineEvalSplitterInfo ${splitter.infoid} not found")
  }

  val timeFormat = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss a z")

  def dateTimeToString(t: DateTime, zoneName: String = "UTC"): String =
    timeFormat.print(t.withZone(DateTimeZone.forID(zoneName)))

  /**
   * Common function to create Offline Evaluation for sim eval or auto tune
   * @param engine Engine
   * @param algoList List of Algo
   * @param metricList List of OfflineEvalMetric
   * @param splitter OfflineEvalSplitter
   * @param evalIteration number of iteration
   * @param tuneid specify offline tune id if this Offine Eval is for auto tune
   * @param the created OfflineEval ID
   */
  def createOfflineEval(engine: Engine, algoList: List[Algo], metricList: List[OfflineEvalMetric], splitter: OfflineEvalSplitter, evalIteration: Int, tuneid: Option[Int] = None): Int = {

    // insert offlineeval record without create time
    val newOfflineEval = OfflineEval(
      id = 0,
      engineid = engine.id,
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
    for (algo <- algoList) {
      // duplicate algo for sim eval
      val algoid = algos.insert(algo.copy(
        id = 0,
        offlineevalid = Option(evalid),
        status = "simeval"
      ))
      Logger.info("Create sim eval algo ID " + algoid)
    }

    for (metric <- metricList) {
      val metricid = offlineEvalMetrics.insert(metric.copy(
        id = 0,
        evalid = evalid
      ))
      Logger.info("Create metric ID " + metricid)
    }

    // create splitter record
    val splitterid = offlineEvalSplitters.insert(splitter.copy(
      evalid = evalid,
      name = ("sim-eval-" + evalid + "-splitter")
    ))
    Logger.info("Create offline eval splitter ID " + splitterid)

    // after all algo and metric info is stored.
    // update offlineeval record with createtime, so scheduler can know it's ready to be picked up
    offlineEvals.update(newOfflineEval.copy(
      id = evalid,
      name = ("sim-eval-" + evalid),
      createtime = Option(DateTime.now)
    ))

    evalid
  }

  /**
   * Delete appdata DB of this appid
   */
  def deleteAppData(appid: Int) = {
    Logger.info("Delete appdata for app ID " + appid)
    appDataUsers.deleteByAppid(appid)
    appDataItems.deleteByAppid(appid)
    appDataU2IActions.deleteByAppid(appid)
  }

  /**
   * Delete training set data of this evalid
   */
  def deleteTrainingSetData(evalid: Int) = {
    Logger.info("Delete training set for offline eval ID " + evalid)
    trainingSetUsers.deleteByAppid(evalid)
    trainingSetItems.deleteByAppid(evalid)
    trainingSetU2IActions.deleteByAppid(evalid)
  }

  /**
   * Delete validation set data of this evalid
   */
  def deleteValidationSetData(evalid: Int) = {
    Logger.info("Delete validation set for offline eval ID " + evalid)
    validationSetUsers.deleteByAppid(evalid)
    validationSetItems.deleteByAppid(evalid)
    validationSetU2IActions.deleteByAppid(evalid)
  }

  /**
   * Delete test set data of this evalid
   */
  def deleteTestSetData(evalid: Int) = {
    Logger.info("Delete test set for offline eval ID " + evalid)
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
    }
  }

  /**
   * Delete this app and the assoicated engines and appdata
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

  /**
   * Delete engine and the associated algos and simevals.
   */
  def deleteEngine(engineid: Int, appid: Int, keepSettings: Boolean) = {

    // delete non-sim eval algos, "simeval" algo is deleted when delete sim eval later
    val engineAlgos = algos.getByEngineid(engineid).filter(!isSimEvalAlgo(_))

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

  /**
   * Delete non-simeval algo and associated modeldata, offlineTune
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

  /**
   * Delete offline tune and associated trainig/validation/test set data, evaluated algos, metrics, eval results, and splitters
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
        Logger.info("Delete metric ID " + metric.id)
        offlineEvalMetrics.delete(metric.id)
      }

      Logger.info("Delete offline eval results of offline eval ID " + evalid)
      offlineEvalResults.deleteByEvalid(evalid)

      val evalSplitters = offlineEvalSplitters.getByEvalid(evalid)
      evalSplitters foreach { splitter =>
        Logger.info("Delete Offline Eval Splitter ID " + splitter.id)
        offlineEvalSplitters.delete(splitter.id)
      }
    }

    if (!keepSettings) {
      Logger.info("Delete offline eval ID " + evalid)
      offlineEvals.delete(evalid)
    }

  }

  /**
   * Delete offline tune and associated param gens and offline evals
   */
  def deleteOfflineTune(tuneid: Int, keepSettings: Boolean) = {

    // delete paramGen
    if (!keepSettings) {
      val tuneParamGens = paramGens.getByTuneid(tuneid)
      tuneParamGens foreach { gen =>
        Logger.info("Delete ParamGen ID " + gen.id)
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

  def hadoopRequiredByApp(appid: Int): Boolean = {
    apps.get(appid) map { app =>
      engines.getByAppid(app.id).foldLeft(false) { (b, a) => b || hadoopRequiredByEngine(a.id) }
    } getOrElse false
  }

  def hadoopRequiredByEngine(engineid: Int): Boolean = {
    engines.get(engineid) map { engine =>
      algos.getByEngineid(engine.id).foldLeft(false) { (b, a) => (b || hadoopRequiredByAlgo(a.id)) }
    } getOrElse false
  }

  def hadoopRequiredByAlgo(algoid: Int): Boolean = {
    algos.get(algoid) map { algo =>
      algoInfos.get(algo.infoid) map { algoinfo =>
        algoinfo.techreq.contains("Hadoop")
      } getOrElse false
    } getOrElse false
  }

  def hadoopRequiredByOfflineEval(evalid: Int): Boolean = {
    offlineEvals.get(evalid) map { oe =>
      algos.getByOfflineEvalid(oe.id).foldLeft(false) { (b, a) => (b || hadoopRequiredByAlgo(a.id)) }
    } getOrElse false
  }

  /**
   * Request scheduler to stop and delete sim eval
   * @return Future[SimpleResult]
   */
  def stopAndDeleteSimEvalScheduler(appid: Int, engineid: Int, evalid: Int) = {

    /** Stop any possible running jobs */
    val stop = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/offlineevals/${evalid}/stop").get()
    /** Clean up intermediate data files */
    val deleteHadoop = if (hadoopRequiredByOfflineEval(evalid)) "?hadoop=1" else ""
    val delete = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/offlineevals/${evalid}/delete${deleteHadoop}").get()
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
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String]))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete simulated evaluation. Please check if the scheduler server is running properly. " + e.getMessage())))
    }

    complete
  }

  /**
   * Request scheduler to stop and delete offline Tune
   * @return Future[SimpleResult]
   */
  def stopAndDeleteOfflineTuneScheduler(appid: Int, engineid: Int, tuneid: Int) = {

    val stop = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/offlinetunes/${tuneid}/stop").get()

    val deletes = offlineEvals.getByTuneid(tuneid) map { eval =>
      val deleteHadoop = if (hadoopRequiredByOfflineEval(eval.id)) "?hadoop=1" else ""
      WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/offlineevals/${eval.id}/delete${deleteHadoop}").get()
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
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String]))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete autotuning algorithm. Please check if the scheduler server is running properly. " + e.getMessage())))
    }

    complete

  }

  /**
   * Request scheduler to delete algo file
   * @return Future[SimpleResult]
   */
  def deleteAlgoScheduler(appid: Int, engineid: Int, id: Int) = {
    val deleteHadoop = if (hadoopRequiredByAlgo(id)) "?hadoop=1" else ""
    val delete = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/algos/${id}/delete${deleteHadoop}").get()

    delete map { r =>
      if (r.status == http.Status.OK)
        Ok
      else
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String]))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete algorithm. Please check if the scheduler server is running properly. " + e.getMessage())))
    }
  }

  /**
   * Request scheduler to delete engine file
   * @return Future[SimpleResult]
   */
  def deleteEngineScheduler(appid: Int, engineid: Int) = {
    val deleteHadoop = if (hadoopRequiredByEngine(engineid)) "?hadoop=1" else ""
    val delete = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/engines/${engineid}/delete${deleteHadoop}").get()

    delete map { r =>
      if (r.status == http.Status.OK)
        Ok
      else
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String]))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete engine. Please check if the scheduler server is running properly. " + e.getMessage())))
    }
  }

  /**
   * Request scheduler to delete app file
   * @return Future[SimpleResult]
   */
  def deleteAppScheduler(appid: Int) = {
    val deleteHadoop = if (hadoopRequiredByApp(appid)) "?hadoop=1" else ""
    val delete = WS.url(s"${settingsSchedulerUrl}/apps/${appid}/delete${deleteHadoop}").get()

    delete map { r =>
      if (r.status == http.Status.OK)
        Ok
      else
        InternalServerError(Json.obj("message" -> (r.json \ "message").as[String]))
    } recover {
      case e: Exception => InternalServerError(Json.obj("message" ->
        ("Failed to delete app. Please check if the scheduler server is running properly. " + e.getMessage())))
    }
  }

  def displayParams(algoInfo: AlgoInfo, params: Map[String, Any]): String = {
    // return default value if the param doesn't exist in algo's params field
    // (eg. new param added later).
    algoInfo.name + ": " + (algoInfo.paramorder map { paramName =>
      algoInfo.params(paramName).name + " = " +
        params.getOrElse(paramName, algoInfo.params(paramName).defaultvalue)
    } mkString (", "))
  }
}