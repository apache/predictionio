package io.prediction.scheduler

import io.prediction.commons._

import collection.JavaConversions._
import play.api._
import play.api.libs.json._
import play.api.mvc._
import org.quartz.impl.matchers.GroupMatcher._;
import org.quartz.impl.StdSchedulerFactory
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey.jobKey
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.TriggerKey.triggerKey
import org.quartz.UnableToInterruptJobException

object Scheduler extends Controller {
  /** Get settings. */
  val config = new Config
  val apps = config.getSettingsApps
  val engines = config.getSettingsEngines
  val engineInfos = config.getSettingsEngineInfos
  val algos = config.getSettingsAlgos
  val algoInfos = config.getSettingsAlgoInfos
  val offlineEvals = config.getSettingsOfflineEvals
  val offlineEvalSplitters = config.getSettingsOfflineEvalSplitters
  val offlineEvalSplitterInfos = config.getSettingsOfflineEvalSplitterInfos
  val offlineEvalMetrics = config.getSettingsOfflineEvalMetrics
  val offlineEvalMetricInfos = config.getSettingsOfflineEvalMetricInfos
  val offlineEvalResults = config.getSettingsOfflineEvalResults
  val itemRecScores = config.getModeldataItemRecScores

  val scheduler = StdSchedulerFactory.getDefaultScheduler()
  val jobTree = new JobTreeJobListener("predictionio-algo")
  scheduler.getListenerManager.addJobListener(jobTree)

  /** Try search path if hadoop home is not set. */
  val hadoopCommand = config.settingsHadoopHome map { h => h+"/bin/hadoop" } getOrElse { "hadoop" }

  def online() = Action { Ok("PredictionIO Scheduler is online.") }

  def userSync(userid: Int) = Action {
    try {
      /** Remove jobs that do not correspond to an algo. */
      scheduler.getJobKeys(groupEquals(Jobs.algoJobGroup)) foreach { jobKey =>
        val algoid = jobKey.getName().toInt
        algos.get(algoid) getOrElse {
          Logger.info("Found job for algo ID " + algoid + " in scheduler but not in settings. Removing job from scheduler.")
          scheduler.deleteJob(jobKey)
        }
      }

      /** Synchronize every app of the user. */
      apps.getByUserid(userid) foreach { app =>
        engines.getByAppid(app.id) foreach { engine =>
          /** Algos. */
          syncAlgoJobs(app, engine, false)

          /** Offline evaluations. */
          offlineEvals.getByEngineid(engine.id) foreach { offlineEval =>
            val offlineEvalid = offlineEval.id.toString
            val triggerkey = triggerKey(offlineEvalid, Jobs.offlineEvalJobGroup)
            offlineEval.createtime foreach { ct =>
              if (scheduler.checkExists(triggerkey) == false) {
                offlineEval.endtime getOrElse {
                  val offlineEvalJob = Jobs.offlineEvalJob(config, app, engine, offlineEval)
                  scheduler.addJob(offlineEvalJob, true)

                  val trigger = newTrigger() forJob(jobKey(offlineEvalid, Jobs.offlineEvalJobGroup)) withIdentity(offlineEvalid, Jobs.offlineEvalJobGroup) startNow() build()
                  scheduler.scheduleJob(trigger)
                }
              }
            }
          }
        }
      }

      /** Complete synchronization. */
      Ok(Json.obj("message" -> "Synchronized algorithms settings with scheduler successfully."))
    } catch {
      case e: RuntimeException => e.printStackTrace; NotFound(Json.obj("message" -> ("Synchronization failed: " + e.getMessage())))
      case e: Exception => InternalServerError(Json.obj("message" -> ("Synchronization failed: " + e.getMessage())))
    }
  }

  /** Run training of deployed algorithms immediately */
  def syncAlgoJobs(app: settings.App, engine: settings.Engine, runoncenow: Boolean = false) = {
    /** Algos. */
    algos.getByEngineid(engine.id) foreach { algo =>
      val logPrefix = s"Algo ID ${algo.id}: "
      algoInfos.get(algo.infoid) map { algoinfo =>
        val algoid = algo.id.toString
        val triggerkey = triggerKey(algoid, Jobs.algoJobGroup)
        if (algo.deployed == true) {
          /** Running once now is independent of whether the trigger exist or not */
          if (runoncenow) {
            Logger.info(s"${logPrefix}Setting up batch algo job (run once now)")
            algoinfo.batchcommands map { batchcommands =>
              val job = Jobs.algoJob(config, app, engine, algo, batchcommands)
              scheduler.addJob(job, true)
              val trigger = newTrigger() forJob(jobKey(algoid, Jobs.algoJobGroup)) withIdentity(s"${algoid}-runonce", Jobs.algoJobGroup) startNow() build()
              scheduler.scheduleJob(trigger)
            } getOrElse {
              Logger.info(s"${logPrefix}Giving up setting up batch algo job because it does not have any batch command")
            }
          } else if (scheduler.checkExists(triggerkey) == false) {
            Logger.info(s"${logPrefix}Setting up batch algo job (run every hour from now)")
            algoinfo.batchcommands map { batchcommands =>
              val job = Jobs.algoJob(config, app, engine, algo, batchcommands)
              scheduler.addJob(job, true)
              val trigger = newTrigger() forJob(jobKey(algoid, Jobs.algoJobGroup)) withIdentity(algoid, Jobs.algoJobGroup) startNow() withSchedule(simpleSchedule() withIntervalInHours(1) repeatForever()) build()
              scheduler.scheduleJob(trigger)
            } getOrElse {
              Logger.info(s"${logPrefix}Giving up setting up batch algo job because it does not have any batch command")
            }
          }
        } else {
          if (scheduler.checkExists(triggerkey) == true) {
            scheduler.unscheduleJob(triggerkey)
          }
        }
      } getOrElse {
        Logger.info(s"${logPrefix}Skipping batch algo job setup because information about this algo (${algo.infoid}) cannot be found")
      }
    }
  }

  def trainEngineOnceNow(appid: Int, engineid: Int) = Action {
    try {
      apps.get(appid) map { app =>
        engines.get(engineid) map { engine =>
          syncAlgoJobs(app, engine, true)
          Ok(Json.obj("message" -> "Immediate engine training request has been accepted."))
        } getOrElse {
          NotFound(Json.obj("message" -> s"Engine ID $engineid is invalid"))
        }
      } getOrElse {
        NotFound(Json.obj("message" -> s"App ID $appid is invalid"))
      }
    } catch {
      case e: RuntimeException => e.printStackTrace; NotFound(Json.obj("message" -> ("Request failed: " + e.getMessage())))
      case e: Exception => InternalServerError(Json.obj("message" -> ("Request failed: " + e.getMessage())))
    }
  }

  def algoStatus(appid: Int, engineid: Int, algoid: Int) = Action {
    if (scheduler.checkExists(jobKey(algoid.toString(), Jobs.algoJobGroup))) {
      /** The following checks only jobs in this particular scheduler node. */
      /** TODO: Clustering support. */
      try {
        val running = scheduler.getCurrentlyExecutingJobs() map { context =>
          val jobDetail = context.getJobDetail()
          val jobKey = jobDetail.getKey()
          jobKey.getName() == algoid.toString()
        } reduce { (a, b) => a || b }
        if (running)
          Ok(Json.obj("algoid" -> algoid, "status" -> "jobrunning"))
        else
          Ok(Json.obj("algoid" -> algoid, "status" -> "jobnotrunning"))
      } catch {
        case e: UnsupportedOperationException => Ok(Json.obj("algoid" -> algoid, "status" -> "jobnotrunning"))
      }
    } else {
      Ok(Json.obj("algoid" -> algoid, "status" -> "jobnotexist"))
    }
  }

  def stopOfflineEval(appid: Int, engineid: Int, offlineevalid: Int) = Action {
    val offlineEvalJobKey = jobKey(offlineevalid.toString(), Jobs.offlineEvalJobGroup)
    if (scheduler.checkExists(offlineEvalJobKey)) {
      /** The following checks only jobs in this particular scheduler node. */
      /** TODO: Clustering support. */
      try {
        val running = scheduler.getCurrentlyExecutingJobs() map { context =>
          val jobDetail = context.getJobDetail()
          val jobKey = jobDetail.getKey()
          jobKey.getName() == offlineevalid.toString()
        } reduce { (a, b) => a || b }
        if (running)
          try {
            scheduler.interrupt(offlineEvalJobKey)
            Ok(Json.obj("offlineevalid" -> offlineevalid, "status" -> "jobkilled"))
          } catch {
            case e: UnableToInterruptJobException => Ok(Json.obj("offlineevalid" -> offlineevalid, "status" -> "jobnotkilled"))
          }
        else
          Ok(Json.obj("offlineevalid" -> offlineevalid, "status" -> "jobnotrunning"))
      } catch {
        case e: UnsupportedOperationException => Ok(Json.obj("offlineevalid" -> offlineevalid, "status" -> "jobnotrunning"))
      }
    } else {
      Ok(Json.obj("offlineevalid" -> offlineevalid, "status" -> "jobnotexist"))
    }
  }
}
