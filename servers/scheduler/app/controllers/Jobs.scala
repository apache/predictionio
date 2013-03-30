package io.prediction.scheduler

import io.prediction.commons._
import io.prediction.commons.filepath._
import io.prediction.commons.settings.{Algo, App, Engine, OfflineEval, OfflineEvalMetric}

import com.github.nscala_time.time.Imports._
import org.clapper.scalasti.StringTemplate
import org.quartz.{DisallowConcurrentExecution, PersistJobDataAfterExecution}
import org.quartz.{InterruptableJob, Job, JobDetail, JobExecutionContext}
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey.jobKey
import org.quartz.jobs.NativeJob

import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.sys.process._

object Jobs {
  val algoJobGroup = "predictionio-algo"
  val offlineEvalJobGroup = "predictionio-offlineeval"
  val offlineEvalSplitJobGroup = "predictionio-offlineeval-split"
  val offlineEvalTrainingJobGroup = "predictionio-offlineeval-training"
  val offlineEvalMetricJobGroup   = "predictionio-offlineeval-metrics"
  val offlineEvalResultsJobGroup = "predictionio-offlineevalresults"
  val offlineEvalSplitCommands = Map(
    "itemrec" -> Seq("$hadoop$ jar $pdioEvalJar$ io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplit --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ $itypes$ --trainingsize $trainingsize$ --testsize $testsize$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$")
  )
  val offlineEvalMetricCommands = Map(
    "itemrec" -> Seq(
      "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
      "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
      "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")
  )

  def algoJob(config: Config, app: App, engine: Engine, algo: Algo, batchcommands: Seq[String]) = {
    /** Build command from template. */
    val command = new StringTemplate(batchcommands.mkString(" && "))

    // get default params value so old algo record still work even after algo default param is updated.
    val algoinfos = config.getSettingsAlgoInfos
    val defaultParams = algoinfos.get(algo.infoid) map { algoinfo =>
      algoinfo.paramdefaults
    } getOrElse {
      throw new RuntimeException("No algo info found for this infoid: " + algo.infoid)
    }

    val params = defaultParams ++ algo.params
    command.setAttributes(params)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    setSharedAttributes(command, config, app, engine, Some(algo), None, None)

    /** Add a job, then build a trigger for it.
      * This is necessary for updating any existing job,
      * and make sure the trigger will fire.
      */
    val job = newJob(classOf[AlgoJob]) withIdentity(algo.id.toString, algoJobGroup) storeDurably(true) build()
    job.getJobDataMap().put("template", command.toString)
    job.getJobDataMap().put("algoid", algo.id)
    job.getJobDataMap().put("enginetype", engine.enginetype)

    job
  }

  /** Offline Evaluation Flow
    *
    * 1. Perform training and test set splitting
    * 2. For each algo to be evaluated
    *    1. Run algo on training set
    *    2. Run all metrics on model data from the above against test set
    * 3. Mark offline evaluation as finished
    */
  def offlineEvalJob(config: Config, app: App, engine: Engine, offlineEval: OfflineEval) = {
    val splitCommand = new StringTemplate(offlineEvalSplitCommands(engine.enginetype).mkString(" && "))
    setSharedAttributes(splitCommand, config, app, engine, None, Some(offlineEval), None)

    /** Add a job, then build a trigger for it.
      * This is necessary for updating any existing job,
      * and make sure the trigger will fire.
      */
    val job = newJob(classOf[OfflineEvalJob]) withIdentity(offlineEval.id.toString, offlineEvalJobGroup) storeDurably(true) build()
    job.getJobDataMap().put("evalid", offlineEval.id)
    job.getJobDataMap().put("splitCommand", splitCommand.toString)
    job.getJobDataMap().put("enginetype", engine.enginetype)

    /** Training algo job. */
    val algosToRun = config.getSettingsAlgos.getByOfflineEvalid(offlineEval.id).toList
    val algoinfos = config.getSettingsAlgoInfos
    val metricsToRun = config.getSettingsOfflineEvalMetrics.getByEvalid(offlineEval.id).toList
    job.getJobDataMap().put("algoids", algosToRun.map(_.id).mkString(","))
    job.getJobDataMap().put("metricids", metricsToRun.map(_.id).mkString(","))

    algosToRun foreach { algo =>
      algoinfos.get(algo.infoid) map { algoinfo =>
        algoinfo.offlineevalcommands map { offlineEvalCommands =>
          val trainingCommand = new StringTemplate(offlineEvalCommands.mkString(" && "))
          setSharedAttributes(trainingCommand, config, app, engine, Some(algo), Some(offlineEval), None)
          job.getJobDataMap().put(s"trainingCommand${algo.id}", trainingCommand.toString)

          /** Metrics. */
          metricsToRun foreach { metric =>
            val metricCommand = new StringTemplate(offlineEvalMetricCommands(engine.enginetype).mkString(" && "))
            setSharedAttributes(metricCommand, config, app, engine, Some(algo), Some(offlineEval), Some(metric))
            job.getJobDataMap().put(s"metricCommand${algo.id}.${metric.id}", metricCommand.toString)
          }
        } getOrElse {
          Logger.info(s"OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id} (${algo.name}): Skipping this algorithm because it does not have any offline evaluation command")
        }
      } getOrElse {
        Logger.info(s"OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id} (${algo.name}): Skipping this algorithm because its information (${algo.infoid}) cannot be found")
      }
    }

    job
  }

  def setSharedAttributes(command: StringTemplate, config: Config, app: App, engine: Engine, algo: Option[Algo], offlineEval: Option[OfflineEval], metric: Option[OfflineEvalMetric]) = {
    /** OfflineEvalMetric-specific attributes */
    metric map { met =>
      command.setAttributes(met.params)
      command.setAttribute("metricid", met.id)
    }

    /** OfflineEval-specific attributes */
    offlineEval map { oe =>
      command.setAttribute("evalid", oe.id)
      command.setAttribute("trainingsize", oe.trainingsize)
      command.setAttribute("testsize", oe.testsize)
      command.setAttribute("modelset", "false")
    }

    /** Algo-specific attributes */
    algo map { alg =>
      command.setAttributes(command.attributes ++ alg.params)
      command.setAttribute("jar", config.getJar(alg.infoid).getOrElse(""))
      command.setAttribute("algoid", alg.id)
      command.setAttribute("mahoutTempDir", BaseDir.algoDir(config.settingsHdfsRoot+"mahout_temp/", app.id, engine.id, alg.id, offlineEval.map(_.id)))
      command.setAttribute("algoDir", BaseDir.algoDir(config.settingsHdfsRoot, app.id, engine.id, alg.id, offlineEval.map(_.id)))
      command.setAttribute("dataFilePrefix", DataFile(config.settingsHdfsRoot, app.id, engine.id, alg.id, offlineEval.map(_.id), ""))
      command.setAttribute("algoFilePrefix", AlgoFile(config.settingsHdfsRoot, app.id, engine.id, alg.id, offlineEval.map(_.id), ""))
      /** Attributes that only apply to batch algo run that are NOT offline evaluations */
      offlineEval getOrElse {
        command.setAttribute("modelset", "$modelset$")
      }
    }

    /** Common attributes */
    command.setAttribute("base", config.base)
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("goalParam", engine.settings("goal"))
    command.setAttribute("pdioEvalJar", config.getJar("io.prediction.evaluations.scalding.itemrec").getOrElse(""))
    command.setAttribute("mahoutCoreJobJar", config.getJar("io.prediction.algorithms.mahout-core-job").getOrElse(""))
    command.setAttribute("itemrecScalaMahoutJar", config.getJar("io.prediction.algorithms.mahout.itemrec").getOrElse(""))
    command.setAttribute("topkJar", config.getJar("io.prediction.evaluations.itemrec.topkitems").getOrElse(""))
    command.setAttribute("configFile", Option(System.getProperty("config.file")).map(c => "-Dconfig.file="+c).getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("hdfsRoot", config.settingsHdfsRoot)
    command.setAttribute("localTempRoot", config.settingsLocalTempRoot)
    command.setAttribute("settingsDbType", config.settingsDbType)
    command.setAttribute("settingsDbName", config.settingsDbName)
    command.setAttribute("settingsDbHost", config.settingsDbHost)
    command.setAttribute("settingsDbPort", config.settingsDbPort)
    command.setAttribute("appdataDbType", config.appdataDbType)
    command.setAttribute("appdataDbName", config.appdataDbName)
    command.setAttribute("appdataDbHost", config.appdataDbHost)
    command.setAttribute("appdataDbPort", config.appdataDbPort)
    command.setAttribute("appdataTrainingDbType", config.appdataTrainingDbType)
    command.setAttribute("appdataTrainingDbName", config.appdataTrainingDbName)
    command.setAttribute("appdataTrainingDbHost", config.appdataTrainingDbHost)
    command.setAttribute("appdataTrainingDbPort", config.appdataTrainingDbPort)
    command.setAttribute("appdataTestDbType", config.appdataTestDbType)
    command.setAttribute("appdataTestDbName", config.appdataTestDbName)
    command.setAttribute("appdataTestDbHost", config.appdataTestDbHost)
    command.setAttribute("appdataTestDbPort", config.appdataTestDbPort)
    command.setAttribute("modeldataDbType", config.modeldataDbType)
    command.setAttribute("modeldataDbName", config.modeldataDbName)
    command.setAttribute("modeldataDbHost", config.modeldataDbHost)
    command.setAttribute("modeldataDbPort", config.modeldataDbPort)
    command.setAttribute("modeldataTrainingDbType", config.modeldataTrainingDbType)
    command.setAttribute("modeldataTrainingDbName", config.modeldataTrainingDbName)
    command.setAttribute("modeldataTrainingDbHost", config.modeldataTrainingDbHost)
    command.setAttribute("modeldataTrainingDbPort", config.modeldataTrainingDbPort)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }
    command.setAttribute("numRecommendations", engine.settings.getOrElse("numRecommendations", 500))
    command.setAttribute("unseenOnly", engine.settings.getOrElse("unseenonly", false))
  }
}

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
class AlgoJob extends Job {
  override def execute(context: JobExecutionContext) = {
    val jobDataMap = context.getMergedJobDataMap
    val algoid = jobDataMap.getInt("algoid")
    val enginetype = jobDataMap.getString("enginetype")
    val template = new StringTemplate(jobDataMap.getString("template"))
    val algos = Scheduler.algos
    val itemRecScores = Scheduler.itemRecScores
    algos.get(algoid) map { algo =>
      Logger.info("Algo ID %d: Current model set for is %s".format(algo.id, algo.modelset))
      Logger.info("Algo ID %d: Launching algo job for model set %s".format(algo.id, !algo.modelset))
      template.setAttribute("modelset", !algo.modelset)
      val command = template.toString
      Logger.info("Algo ID %d: Going to run: %s".format(algo.id, command))
      val code = command.split("&&").map(c => Process(c.trim)).reduceLeft((a, b) => a #&& b).!
      if (code == 0) {
        Logger.info("Algo ID %d: Flipping model set flag to %s".format(algo.id, !algo.modelset))
        algos.update(algo.copy(modelset = !algo.modelset))
        enginetype match {
          case "itemrec" => {
            Logger.info("Algo ID %d: Deleting data of model set %s".format(algo.id, algo.modelset))
            itemRecScores.deleteByAlgoidAndModelset(algo.id, algo.modelset)
            Logger.info("Algo ID %d: Deletion completed".format(algo.id))
          }
        }
        Logger.info("Algo ID %d: Job completed".format(algo.id))
      } else {
        Logger.warn("Algo ID %d: Not flipping model set flag because the algo job returned non-zero exit code".format(algo.id))
      }
    } getOrElse Logger.warn("Algo ID %d: No job to run because the algo cannot be found from the database".format(algoid))
  }
}

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
class OfflineEvalJob extends InterruptableJob {
  var kill = false

  override def execute(context: JobExecutionContext): Unit = {
    val jobDataMap = context.getMergedJobDataMap
    val evalid = jobDataMap.getInt("evalid")
    val algoids = jobDataMap.getString("algoids").split(",") map { _.toInt }
    val metricids = jobDataMap.getString("metricids").split(",") map { _.toInt }
    val splitCommand = jobDataMap.getString("splitCommand")
    val enginetype = jobDataMap.getString("enginetype")

    val offlineEvals = Scheduler.offlineEvals

    val logPrefix = s"OfflineEval ID $evalid: "

    /** Synchronization flags */
    var splittingDone = false
    var splittingCode = 0

    offlineEvals.get(evalid) map { offlineEval =>
      Logger.info(s"${logPrefix}Starting offline evaluation")
      /** Mark the start time */
      offlineEvals.update(offlineEval.copy(starttime = Some(DateTime.now)))

      /** Delete old model data, if any (usually recovering from an incomplete run) */
      enginetype match {
        case "itemrec" => algoids foreach { algoid =>
          Logger.info(s"${logPrefix}Algo ID $algoid: Deleting any old model data")
          Scheduler.itemRecScores.deleteByAlgoid(algoid)
        }
      }

      /** Run and wait on splitting */
      Logger.info(s"${logPrefix}(Split) Going to run: $splitCommand")
      val splitProc = splitCommand.split("&&").map(c => Process(c.trim)).reduceLeft((a, b) => a #&& b).run
      future {
        Logger.info(s"${logPrefix}(Split) Scheduler waiting for sub-process to finish")
        splittingCode = splitProc.exitValue
        splittingDone = true
        Logger.info(s"${logPrefix}(Split) Sub-process has finished")
      }
      while (!splittingDone) {
        if (kill) {
          Logger.info(s"${logPrefix}(Split) Received termination signal")
          Logger.info(s"${logPrefix}Aborting offline evaluation")
          splitProc.destroy
          return
        }
        if (splittingCode != 0) {
          Logger.warn(s"${logPrefix}(Split) Job failed with exit code: $splittingCode")
          Logger.info(s"${logPrefix}Aborting offline evaluation")
          splitProc.destroy
          return
        }
        Thread.sleep(1000)
      }

      val trainingCode = collection.mutable.Map[Int, Int]()
      val trainingProc = collection.mutable.Map[Int, Process]()
      val metricCode = collection.mutable.Map[String, Int]()
      val metricDone = collection.mutable.Map[String, Boolean]()
      val metricProc = collection.mutable.Map[String, Process]()

      /** Run and wait on training before starting metrics */
      algoids foreach { algoid =>
        /** Populate data maps first */
        trainingCode(algoid) = 0
        metricids foreach { metricid =>
          metricCode(s"${algoid}.${metricid}") = 0
          metricDone(s"${algoid}.${metricid}") = false
        }

        val algoLogPrefix = s"Algo ID $algoid: "
        val trainingCommand = jobDataMap.getString(s"trainingCommand${algoid}")
        Logger.info(s"${logPrefix}${algoLogPrefix}(Training) Going to run: $trainingCommand")
        trainingProc(algoid) = trainingCommand.split("&&").map(c => Process(c.trim)).reduceLeft((a, b) => a #&& b).run
        future {
          Logger.info(s"${logPrefix}${algoLogPrefix}(Training) Scheduler waiting for sub-process to finish")
          val trainingCode = trainingProc(algoid).exitValue
          if (trainingCode != 0) {
            Logger.warn(s"${logPrefix}${algoLogPrefix}(Training) Job failed with exit code: $trainingCode")
            Logger.info(s"${logPrefix}${algoLogPrefix}(Training) Aborting any subsequent metric jobs")
          } else {
            Logger.info(s"${logPrefix}${algoLogPrefix}(Training) Sub-process has finished")
          }

          /** Run metrics */
          metricids foreach { metricid =>
            val algometricid = s"${algoid}.${metricid}"
            if (trainingCode != 0) {
              /** Skip all metrics with this algo if algo failed */
              metricDone(algometricid) = true
            } else {
              val metricLogPrefix = s"Metric ID $metricid: "
              val metricCommand = jobDataMap.getString(s"metricCommand${algoid}.${metricid}")
              Logger.info(s"${logPrefix}${algoLogPrefix}${metricLogPrefix}(Metric) Going to run: $metricCommand")
              metricProc(algometricid) = metricCommand.split("&&").map(c => Process(c.trim)).reduceLeft((a, b) => a #&& b).run
              future {
                Logger.info(s"${logPrefix}${algoLogPrefix}${metricLogPrefix}(Metric) Scheduler waiting for sub-process to finish")
                val metCode = metricProc(algometricid).exitValue
                metricCode(algometricid) = metCode
                metricDone(algometricid) = true
                if (metCode != 0) {
                  Logger.info(s"${logPrefix}${algoLogPrefix}${metricLogPrefix}(Metric) Job failed with exit code: $metCode")
                } else {
                  Logger.info(s"${logPrefix}${algoLogPrefix}${metricLogPrefix}(Metric) Sub-process has finished")
                }
              }
            }
          }
        }
      }
      while (!metricDone.values.reduce((a, b) => a && b)) {
        if (kill) {
          Logger.info(s"${logPrefix}(Training/Metric) Received termination signal")
          Logger.info(s"${logPrefix}Aborting offline evaluation")
          trainingProc.values.map(_.destroy)
          metricProc.values.map(_.destroy)
          return
        }
        Thread.sleep(1000)
      }

      /** Check for errors from metric */
      val trainingErrors = trainingCode.values.sum
      val metricErrors = metricCode.values.sum
      if (trainingErrors + metricErrors != 0)
        Logger.warn(s"${logPrefix}Offline evaluation completed with error(s)")
      else
        Logger.info(s"${logPrefix}Offline evaluation completed")
      /** Mark the end time since this is used to determine whether the run has finished */
      offlineEvals.update(offlineEval.copy(endtime = Some(DateTime.now)))
    } getOrElse {
      Logger.info(s"${logPrefix}Not starting offline evaluation because the offline evaluation cannot be found from the database")
    }
  }

  override def interrupt() = {
    kill = true
  }
}
