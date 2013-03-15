package io.prediction.scheduler

import io.prediction.commons._
import io.prediction.commons.filepath.ModelDataDir

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

object Scheduler extends Controller {
  /** Get settings. */
  val settingsConfig = new settings.Config
  val apps = settingsConfig.getApps
  val engines = settingsConfig.getEngines
  val algos = settingsConfig.getAlgos
  val offlineEvals = settingsConfig.getOfflineEvals
  val offlineEvalMetrics = settingsConfig.getOfflineEvalMetrics
  val algoinfos = settingsConfig.getAlgoInfos

  val appdataConfig = new appdata.Config

  val modeldataConfig = new modeldata.Config
  val modeldataTrainingSetConfig = new modeldata.TrainingSetConfig

  val scheduler = StdSchedulerFactory.getDefaultScheduler()
  val jobTree = new JobTreeJobListener("predictionio-algo")
  scheduler.getListenerManager.addJobListener(jobTree)

  /** Try search path if hadoop home is not set. */
  val hadoopCommand = settingsConfig.settingsHadoopHome map { h => h+"/bin/hadoop" } getOrElse { "hadoop" }

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
          algos.getByEngineid(engine.id) foreach { algo =>
            algoinfos.get(algo.infoid) map { algoinfo =>
              val algoid = algo.id.toString
              val triggerkey = triggerKey(algoid, Jobs.algoJobGroup)
              if (algo.deployed == true) {
                if (scheduler.checkExists(triggerkey) == false) {
                  Logger.info("Algo ID %d: Setting up batch algo job".format(algo.id))
                  algoinfo.batchcommands map { batchcommands =>
                    val job = Jobs.algoJobs(settingsConfig, appdataConfig, modeldataConfig, app, engine, algo, batchcommands)
                    scheduler.addJob(job, true)

                    val trigger = newTrigger() forJob(jobKey(algoid, Jobs.algoJobGroup)) withIdentity(algoid, Jobs.algoJobGroup) startNow() withSchedule(simpleSchedule() withIntervalInHours(1) repeatForever()) build()
                    scheduler.scheduleJob(trigger)
                  } getOrElse {
                    Logger.info("Giving up setting up batch algo job because it does not have any batch command.")
                  }
                }
              } else {
                if (scheduler.checkExists(triggerkey) == true) {
                  scheduler.unscheduleJob(triggerkey)
                }
              }
            } getOrElse {
              Logger.info("Algo ID %d: Skipping batch algo job setup because information about this algo (%s) cannot be found".format(algo.id, algo.infoid))
            }
          }

          /** Offline evaluations. */
          offlineEvals.getByEngineid(engine.id) foreach { offlineEval =>
            val offlineEvalid = offlineEval.id.toString
            val triggerkey = triggerKey(offlineEvalid, Jobs.offlineEvalJobGroup)
            offlineEval.createtime foreach { ct =>
              if (scheduler.checkExists(triggerkey) == false) {
                offlineEval.endtime match {
                  case None => {
                    Logger.info("Setting up offline evaluation splitting job. Eval ID: %d.".format(offlineEval.id))
                    /** Add a job, then build a trigger for it.
                      * This is necessary for updating any existing job,
                      * and make sure the trigger will fire.
                      */
                    val offlineEvalJob = newJob(classOf[OfflineEvalStartJob]) withIdentity(offlineEvalid, Jobs.offlineEvalJobGroup) build()
                    offlineEvalJob.getJobDataMap().put("evalid", offlineEval.id)
                    scheduler.addJob(offlineEvalJob, true)

                    val offlineEvalSplitJob = Jobs.offlineEvalSplitJob(
                      settingsConfig,
                      appdataConfig,
                      app,
                      engine,
                      offlineEval)
                    scheduler.addJob(offlineEvalSplitJob, true) // this is needed to update the job
                    jobTree.addJobTreeLink(offlineEvalJob.getKey, offlineEvalSplitJob.getKey)

                    /** Training algo job. */
                    val algosToRun = algos.getByOfflineEvalid(offlineEval.id).toList
                    val metricsToRun = offlineEvalMetrics.getByEvalid(offlineEval.id).toList

                    algosToRun foreach { algo =>
                      algoinfos.get(algo.infoid) map { algoinfo =>
                        Logger.info("Setting up offline evaluation training job. Eval ID: %d. Algo ID: %d.".format(offlineEval.id, algo.id))
                        algoinfo.offlineevalcommands map { offlineEvalCommands =>
                          val offlineEvalTrainingJob = Jobs.offlineEvalTrainingJob(
                            settingsConfig,
                            appdataConfig,
                            modeldataTrainingSetConfig,
                            app,
                            engine,
                            algo,
                            offlineEval,
                            offlineEvalCommands)

                          scheduler.addJob(offlineEvalTrainingJob, true) // this is needed to update the job

                          jobTree.addJobTreeLink(offlineEvalSplitJob.getKey, offlineEvalTrainingJob.getKey)

                          /** Metrics. */
                          metricsToRun foreach { metric =>
                            Logger.info("Setting up offline evaluation metric job. Eval ID: %d. Algo ID: %d. Metric ID: %d.".format(offlineEval.id, algo.id, metric.id))

                            val offlineEvalMetricJob = Jobs.offlineEvalMetricJob(
                              settingsConfig,
                              appdataConfig,
                              modeldataTrainingSetConfig,
                              app,
                              engine,
                              algo,
                              offlineEval,
                              metric)

                            scheduler.addJob(offlineEvalMetricJob, true) // this is needed to update the job

                            jobTree.addJobTreeLink(offlineEvalTrainingJob.getKey, offlineEvalMetricJob.getKey)
                          }
                        } getOrElse {
                          Logger.info("Giving up setting up offline evaluation training algo job because it does not have any offline evaluation command.")
                        }
                      } getOrElse {
                        Logger.info("Algo ID %d: Skipping batch algo job setup because information about this algo (%s) cannot be found".format(algo.id, algo.infoid))
                      }
                    }

                    /** Schedule job to poll for results. */
                    Logger.info("Setting up offline evaluation results polling job. Eval ID: %d.".format(offlineEval.id))
                    val pollOfflineEvalResultsJob = newJob(classOf[PollOfflineEvalResultsJob]) withIdentity(offlineEvalid, Jobs.offlineEvalResultsJobGroup) build()
                    pollOfflineEvalResultsJob.getJobDataMap().put("evalid", offlineEvalid)
                    pollOfflineEvalResultsJob.getJobDataMap().put("metricids", metricsToRun.map(_.id).mkString(","))
                    pollOfflineEvalResultsJob.getJobDataMap().put("algoids", algosToRun.map(_.id).mkString(","))
                    scheduler.addJob(pollOfflineEvalResultsJob, true)
                    jobTree.addJobTreeLink(offlineEvalJob.getKey, pollOfflineEvalResultsJob.getKey)

                    /** Schedule the first step of the offline evaluation job after everything is done. */
                    val trigger = newTrigger() forJob(jobKey(offlineEvalid, Jobs.offlineEvalJobGroup)) withIdentity(offlineEvalid, Jobs.offlineEvalJobGroup) startNow() build()
                    scheduler.scheduleJob(trigger)
                  }
                  case Some(et) => Logger.info("Offline evaluation (ID: %d) has already finished at %s.".format(offlineEval.id, et))
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
}
