package io.prediction.scheduler

import io.prediction.commons._
import io.prediction.commons.filepath.ModelDataDir

import collection.JavaConversions._
import java.io.File
import java.util.Date
import play.api._
import play.api.libs.json._
import play.api.mvc._
import org.apache.commons.io.FileUtils
import org.clapper.scalasti.StringTemplate
import org.quartz.DisallowConcurrentExecution
import org.quartz.{Job, JobDetail, JobExecutionContext}
import org.quartz.impl.matchers.GroupMatcher._;
import org.quartz.impl.StdSchedulerFactory
import org.quartz.jobs.NativeJob
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey.jobKey
import org.quartz.listeners.JobChainingJobListener
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.TriggerKey.triggerKey

object Scheduler extends Controller {
  val scheduler = StdSchedulerFactory.getDefaultScheduler()
  val jobChain = new JobChainingJobListener("predictionio-algo")
  scheduler.getListenerManager.addJobListener(jobChain)

  val algoCommands = Map(
    "io.prediction.algorithms.scalding.itemrec.knnitembased" -> (
      "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$ && " +
      "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ && " +
      "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$"
    )
  )

  val offlineEvalSplitCommands = Map(
    "itemrec" -> "hadoop jar $jar$ io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplit --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ $itypes$ --trainingsize $trainingsize$ --testsize $testsize$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$"
  )

  val offlineEvalTrainingCommands = Map(
    "io.prediction.algorithms.scalding.itemrec.knnitembased" -> (
      "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$ && " +
      "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ && " +
      "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType file --dbName $modeldatadir$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false"
    )
  )

  val offlineEvalMetricCommands = Map(
    "itemrec" -> (
      "hadoop jar $jar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType file --modeldata_dbName $modeldatadir$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$ && " +
      "hadoop jar $jar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$"
    )
  )

  def userSync(userid: Int) = Action {
    try {
      /** Get settings. */
      val settingsConfig = new settings.Config
      val apps = settingsConfig.getApps
      val engines = settingsConfig.getEngines
      val algos = settingsConfig.getAlgos
      val offlineEvals = settingsConfig.getOfflineEvals
      val offlineEvalMetrics = settingsConfig.getOfflineEvalMetrics

      val appdataConfig = new appdata.Config

      val modeldataConfig = new modeldata.Config

      val algoJobGroup = "predictionio-algo-" + userid
      val algoPostProcessJobGroup = "predictionio-algo-postprocess-" + userid

      val offlineEvalJobGroup = "predictionio-offlineeval-" + userid
      val offlineEvalTrainingJobGroup = "predictionio-offlineeval-training-" + userid
      val offlineEvalMetricJobGroup = "predictionio-offlineeval-metrics-" + userid

      try {
        /** Remove jobs that do not correspond to an algo. */
        scheduler.getJobKeys(groupEquals(algoJobGroup)) foreach { jobKey =>
          val algoid = jobKey.getName().toInt
          algos.get(algoid) getOrElse {
            Logger.info("Found job for algo ID " + algoid + " in scheduler but not in settings. Removing job from scheduler.")
            scheduler.deleteJob(jobKey)
          }
        }
        scheduler.getJobKeys(groupEquals(algoPostProcessJobGroup)) foreach { jobKey =>
          val algoid = jobKey.getName().toInt
          algos.get(algoid) getOrElse {
            Logger.info("Found postprocess job for algo ID " + algoid + " in scheduler but not in settings. Removing job from scheduler.")
            scheduler.deleteJob(jobKey)
          }
        }

        /** Synchronize every app of the user. */
        apps.getByUserid(userid) foreach { app =>
          engines.getByAppid(app.id) foreach { engine =>
            /** Algos. */
            algos.getByEngineid(engine.id) foreach { algo =>
              val algoid = algo.id.toString
              val triggerkey = triggerKey(algoid, algoJobGroup)
              if (algo.deployed == true) {
                if (scheduler.checkExists(triggerkey) == false) {
                  /** Build command from template. */
                  val command = new StringTemplate(algoCommands(algo.pkgname))
                  command.setAttributes(algo.params)
                  engine.itypes foreach { it =>
                    command.setAttribute("itypes", "--itypes" + it.mkString(" "))
                  }

                  /** Fill in settings values. */
                  command.setAttribute("jar", settingsConfig.getJar(algo.pkgname).get)
                  command.setAttribute("appid", app.id)
                  command.setAttribute("engineid", engine.id)
                  command.setAttribute("algoid", algo.id)
                  command.setAttribute("modelset", !algo.modelset)
                  command.setAttribute("hdfsRoot", settingsConfig.settingsHdfsRoot)
                  command.setAttribute("appdataDbType", appdataConfig.appdataDbType)
                  command.setAttribute("appdataDbName", appdataConfig.appdataDbName)
                  command.setAttribute("appdataDbHost", appdataConfig.appdataDbHost)
                  command.setAttribute("appdataDbPort", appdataConfig.appdataDbPort)
                  command.setAttribute("modeldataDbType", modeldataConfig.modeldataDbType)
                  command.setAttribute("modeldataDbName", modeldataConfig.modeldataDbName)
                  command.setAttribute("modeldataDbHost", modeldataConfig.modeldataDbHost)
                  command.setAttribute("modeldataDbPort", modeldataConfig.modeldataDbPort)

                  /** Add a job, then build a trigger for it.
                    * This is necessary for updating any existing job,
                    * and make sure the trigger will fire.
                    */
                  val job = newJob(classOf[SeqNativeJob]) withIdentity(algoid, algoJobGroup) build()
                  job.getJobDataMap().put(
                    NativeJob.PROP_COMMAND,
                    command.toString
                  )
                  scheduler.addJob(job, true) // this is needed to update the job

                  val postProcessJob = newJob(classOf[FlipModelSet]) withIdentity(algoid, algoPostProcessJobGroup) build()
                  postProcessJob.getJobDataMap().put("algoid", algo.id)
                  scheduler.addJob(postProcessJob, true)

                  jobChain.addJobChainLink(job.getKey, postProcessJob.getKey)

                  val trigger = newTrigger() forJob(jobKey(algoid, algoJobGroup)) withIdentity(algoid, algoJobGroup) startNow() withSchedule(simpleSchedule() withIntervalInHours(1) repeatForever()) build()
                  scheduler.scheduleJob(trigger)
                }
              } else {
                if (scheduler.checkExists(triggerkey) == true) {
                  scheduler.unscheduleJob(triggerkey)
                }
              }
            }

            /** Offline evaluations. */
            offlineEvals.getByEngineid(engine.id) foreach { offlineEval =>
              val offlineEvalid = offlineEval.id.toString
              val triggerkey = triggerKey(offlineEvalid, offlineEvalJobGroup)
              offlineEval.createtime foreach { ct =>
                if (scheduler.checkExists(triggerkey) == false) {
                  Logger.info("Setting up offline evaluation splitting job. Eval ID: %d.".format(offlineEval.id))
                  /** Build command from template. */
                  val command = new StringTemplate(offlineEvalSplitCommands(engine.enginetype))
                  engine.itypes foreach { it =>
                    command.setAttribute("itypes", "--itypes" + it.mkString(" "))
                  }

                  /** Fill in settings values. */
                  command.setAttribute("jar", settingsConfig.getJar("io.prediction.evaluations.scalding.itemrec.trainingtestsplit").get)
                  command.setAttribute("appid", app.id)
                  command.setAttribute("engineid", engine.id)
                  command.setAttribute("evalid", offlineEvalid)
                  command.setAttribute("trainingsize", offlineEval.trainingsize)
                  command.setAttribute("testsize", offlineEval.testsize)
                  command.setAttribute("hdfsRoot", settingsConfig.settingsHdfsRoot)
                  command.setAttribute("appdataDbType", appdataConfig.appdataDbType)
                  command.setAttribute("appdataDbName", appdataConfig.appdataDbName)
                  command.setAttribute("appdataDbHost", appdataConfig.appdataDbHost)
                  command.setAttribute("appdataDbPort", appdataConfig.appdataDbPort)
                  command.setAttribute("appdataTrainingDbType", appdataConfig.appdataTrainingDbType)
                  command.setAttribute("appdataTrainingDbName", appdataConfig.appdataTrainingDbName)
                  command.setAttribute("appdataTrainingDbHost", appdataConfig.appdataTrainingDbHost)
                  command.setAttribute("appdataTrainingDbPort", appdataConfig.appdataTrainingDbPort)
                  command.setAttribute("appdataTestDbType", appdataConfig.appdataTestDbType)
                  command.setAttribute("appdataTestDbName", appdataConfig.appdataTestDbName)
                  command.setAttribute("appdataTestDbHost", appdataConfig.appdataTestDbHost)
                  command.setAttribute("appdataTestDbPort", appdataConfig.appdataTestDbPort)

                  /** Add a job, then build a trigger for it.
                    * This is necessary for updating any existing job,
                    * and make sure the trigger will fire.
                    */
                  val offlineEvalSplitJob = newJob(classOf[SeqNativeJob]) withIdentity(offlineEvalid, offlineEvalJobGroup) build()
                  offlineEvalSplitJob.getJobDataMap().put(
                    NativeJob.PROP_COMMAND,
                    command.toString
                  )
                  scheduler.addJob(offlineEvalSplitJob, true) // this is needed to update the job

                  /** Training algo job. */
                  algos.getByOfflineEvalid(offlineEval.id) foreach { algo =>
                    Logger.info("Setting up offline evaluation training job. Eval ID: %d. Algo ID: %d.".format(offlineEval.id, algo.id))
                    val algoid = algo.id.toString
                    val command = new StringTemplate(offlineEvalTrainingCommands(algo.pkgname))
                    command.setAttributes(algo.params)
                    engine.itypes foreach { it =>
                      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
                    }

                    /** Fill in settings values. */
                    command.setAttribute("jar", settingsConfig.getJar(algo.pkgname).get)
                    command.setAttribute("appid", app.id)
                    command.setAttribute("engineid", engine.id)
                    command.setAttribute("algoid", algo.id)
                    command.setAttribute("evalid", offlineEvalid)
                    command.setAttribute("hdfsRoot", settingsConfig.settingsHdfsRoot)
                    command.setAttribute("appdataTrainingDbType", appdataConfig.appdataTrainingDbType)
                    command.setAttribute("appdataTrainingDbName", appdataConfig.appdataTrainingDbName)
                    command.setAttribute("appdataTrainingDbHost", appdataConfig.appdataTrainingDbHost)
                    command.setAttribute("appdataTrainingDbPort", appdataConfig.appdataTrainingDbPort)
                    command.setAttribute("modeldatadir", ModelDataDir(
                      settingsConfig.settingsHdfsRoot,
                      app.id,
                      engine.id,
                      algo.id,
                      Some(offlineEval.id)
                    ))

                    /** Add a job, then build a trigger for it.
                      * This is necessary for updating any existing job,
                      * and make sure the trigger will fire.
                      */
                    val offlineEvalTrainingJob = newJob(classOf[SeqNativeJob]) withIdentity(algoid, offlineEvalTrainingJobGroup) build()
                    offlineEvalTrainingJob.getJobDataMap().put(
                      NativeJob.PROP_COMMAND,
                      command.toString
                    )
                    scheduler.addJob(offlineEvalTrainingJob, true) // this is needed to update the job

                    jobChain.addJobChainLink(offlineEvalSplitJob.getKey, offlineEvalTrainingJob.getKey)

                    /** Metrics. */
                    offlineEvalMetrics.getByEvalid(offlineEval.id) foreach { metric =>
                      Logger.info("Setting up offline evaluation metric job. Eval ID: %d. Algo ID: %d. Metric ID: %d.".format(offlineEval.id, algo.id, metric.id))
                      val command = new StringTemplate(offlineEvalMetricCommands(engine.enginetype))
                      command.setAttributes(algo.params)
                      engine.itypes foreach { it =>
                        command.setAttribute("itypes", "--itypes" + it.mkString(" "))
                      }

                      /** Fill in settings values. */
                      command.setAttributes(metric.params)
                      command.setAttribute("goalParam", engine.settings("goal"))
                      command.setAttribute("jar", settingsConfig.getJar(algo.pkgname).get)
                      command.setAttribute("appid", app.id)
                      command.setAttribute("engineid", engine.id)
                      command.setAttribute("algoid", algo.id)
                      command.setAttribute("evalid", offlineEvalid)
                      command.setAttribute("metricid", metric.id)
                      command.setAttribute("hdfsRoot", settingsConfig.settingsHdfsRoot)
                      command.setAttribute("settingsDbType", settingsConfig.settingsDbType)
                      command.setAttribute("settingsDbName", settingsConfig.settingsDbName)
                      command.setAttribute("settingsDbHost", settingsConfig.settingsDbHost)
                      command.setAttribute("settingsDbPort", settingsConfig.settingsDbPort)
                      command.setAttribute("appdataTrainingDbType", appdataConfig.appdataTrainingDbType)
                      command.setAttribute("appdataTrainingDbName", appdataConfig.appdataTrainingDbName)
                      command.setAttribute("appdataTrainingDbHost", appdataConfig.appdataTrainingDbHost)
                      command.setAttribute("appdataTrainingDbPort", appdataConfig.appdataTrainingDbPort)
                      command.setAttribute("appdataTestDbType", appdataConfig.appdataTestDbType)
                      command.setAttribute("appdataTestDbName", appdataConfig.appdataTestDbName)
                      command.setAttribute("appdataTestDbHost", appdataConfig.appdataTestDbHost)
                      command.setAttribute("appdataTestDbPort", appdataConfig.appdataTestDbPort)
                      command.setAttribute("modeldatadir", ModelDataDir(
                        settingsConfig.settingsHdfsRoot,
                        app.id,
                        engine.id,
                        algo.id,
                        Some(offlineEval.id)
                      ))

                      /** Add a job, then build a trigger for it.
                        * This is necessary for updating any existing job,
                        * and make sure the trigger will fire.
                        */
                      val offlineEvalMetricJob = newJob(classOf[SeqNativeJob]) withIdentity(algoid, offlineEvalMetricJobGroup) build()
                      offlineEvalMetricJob.getJobDataMap().put(
                        NativeJob.PROP_COMMAND,
                        command.toString
                      )
                      scheduler.addJob(offlineEvalMetricJob, true) // this is needed to update the job

                      jobChain.addJobChainLink(offlineEvalTrainingJob.getKey, offlineEvalMetricJob.getKey)
                    }
                  }

                  /** Schedule the first offline evaluation job after everything is done. */
                  val trigger = newTrigger() forJob(jobKey(offlineEvalid, offlineEvalJobGroup)) withIdentity(offlineEvalid, offlineEvalJobGroup) startNow() build()
                  scheduler.scheduleJob(trigger)
                }
              }
            }
          }
        }

        /** Complete synchronization. */
        Ok(Json.toJson(
          Map(
            "message" -> Json.toJson("Synchronized algorithms settings with scheduler successfully.")
          )
        ))
      } catch {
        case e: RuntimeException => NotFound(Json.toJson(Map(
          "message" -> Json.toJson("Synchronization failed: " + e.getMessage())
        )))
      }
    } catch {
      case e: Exception => InternalServerError(Json.toJson(
        Map(
          "message" -> Json.toJson("Synchronization failed: " + e.getMessage())
        )
      ))
    }
  }
}

class FlipModelSet extends Job {
  def execute(context: JobExecutionContext) = {
    val data = context.getJobDetail.getJobDataMap
    val algoid = data.getInt("algoid")
    val config = new settings.Config
    val algos = config.getAlgos
    algos.get(algoid) foreach { algo =>
      algos.update(algo.copy(modelset = !algo.modelset))
    }
  }
}

@DisallowConcurrentExecution
class SeqNativeJob extends NativeJob
