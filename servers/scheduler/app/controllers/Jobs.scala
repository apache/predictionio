package io.prediction.scheduler

import io.prediction.commons._
import io.prediction.commons.filepath._
import io.prediction.commons.settings.{Algo, App, Engine, OfflineEval, OfflineEvalMetric}

import com.github.nscala_time.time.Imports._
import org.clapper.scalasti.StringTemplate
import org.quartz.{DisallowConcurrentExecution, PersistJobDataAfterExecution}
import org.quartz.{Job, JobDetail, JobExecutionContext}
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey.jobKey
import org.quartz.jobs.NativeJob

import play.api.Logger

import scala.sys.process._

object Jobs {
  val algoJobGroup = "predictionio-algo"
  val algoPostProcessJobGroup = "predictionio-algo-postprocess"
  val offlineEvalJobGroup = "predictionio-offlineeval"
  val offlineEvalSplitJobGroup = "predictionio-offlineeval-split"
  val offlineEvalTrainingJobGroup = "predictionio-offlineeval-training"
  val offlineEvalMetricJobGroup   = "predictionio-offlineeval-metrics"
  val offlineEvalResultsJobGroup = "predictionio-offlineevalresults"
  val offlineEvalSplitCommands = Map(
    "itemrec" -> "$hadoop$ jar $pdioEvalJar$ io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplit --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ $itypes$ --trainingsize $trainingsize$ --testsize $testsize$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$"
  )
  val offlineEvalMetricCommands = Map(
    "itemrec" -> (
      "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataDbType$ --modeldata_dbName $modeldataDbName$ --modeldata_dbHost $modeldataDbHost$ --modeldata_dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$ && " +
      "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$ && " +
      "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$"
    )
  )

  def algoJobs(config: Config, app: App, engine: Engine, algo: Algo, batchcommands: Seq[String]) = {
    /** Build command from template. */
    val command = new StringTemplate(batchcommands.mkString(" && "))
    command.setAttributes(algo.params)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttribute("base", config.base)
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("numRecommendations", engine.settings.getOrElse("numRecommendations", 500))
    command.setAttribute("unseenOnly", engine.settings.getOrElse("unseenonly", false))
    command.setAttribute("jar", config.getJar(algo.infoid).getOrElse(""))
    command.setAttribute("pdioEvalJar", config.getJar("io.prediction.evaluations.scalding.itemrec").getOrElse(""))
    command.setAttribute("mahoutJar", config.getJar("io.prediction.algorithms.mahout").getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("algoid", algo.id)
    command.setAttribute("modelset", "$modelset$")
    command.setAttribute("hdfsRoot", config.settingsHdfsRoot)
    command.setAttribute("appdataDbType", config.appdataDbType)
    command.setAttribute("appdataDbName", config.appdataDbName)
    command.setAttribute("appdataDbHost", config.appdataDbHost)
    command.setAttribute("appdataDbPort", config.appdataDbPort)
    command.setAttribute("modeldataDbType", config.modeldataDbType)
    command.setAttribute("modeldataDbName", config.modeldataDbName)
    command.setAttribute("modeldataDbHost", config.modeldataDbHost)
    command.setAttribute("modeldataDbPort", config.modeldataDbPort)
    command.setAttribute("mahoutTempDir", BaseDir.algoDir(config.settingsHdfsRoot+"mahout_temp/", app.id, engine.id, algo.id, None))
    command.setAttribute("algoDir", BaseDir.algoDir(config.settingsHdfsRoot, app.id, engine.id, algo.id, None))
    command.setAttribute("dataFilePrefix", DataFile(config.settingsHdfsRoot, app.id, engine.id, algo.id, None, ""))
    command.setAttribute("algoFilePrefix", AlgoFile(config.settingsHdfsRoot, app.id, engine.id, algo.id, None, ""))

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

  def offlineEvalSplitJob(config: Config, app: App, engine: Engine, offlineEval: OfflineEval) = {
    /** Build command from template. */
    val command = new StringTemplate(offlineEvalSplitCommands(engine.enginetype))
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttribute("base", config.base)
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("pdioEvalJar", config.getJar("io.prediction.evaluations.scalding.itemrec").getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("evalid", offlineEval.id)
    command.setAttribute("trainingsize", offlineEval.trainingsize)
    command.setAttribute("testsize", offlineEval.testsize)
    command.setAttribute("hdfsRoot", config.settingsHdfsRoot)
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

    val offlineEvalSplitJob = newJob(classOf[SeqNativeJob]) withIdentity(offlineEval.id.toString, offlineEvalSplitJobGroup) storeDurably(true) build()
    offlineEvalSplitJob.getJobDataMap().put(
      NativeJob.PROP_COMMAND,
      command.toString
    )

    offlineEvalSplitJob
  }

  def offlineEvalTrainingJob(config: Config, app: App, engine: Engine, algo: Algo, offlineEval: OfflineEval, offlineEvalCommands: Seq[String]): JobDetail = {
    val command = new StringTemplate(offlineEvalCommands.mkString(" && "))
    command.setAttributes(algo.params)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttribute("base", config.base)
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("numRecommendations", engine.settings.getOrElse("numRecommendations", 500))
    command.setAttribute("unseenOnly", engine.settings.getOrElse("unseenonly", false))
    command.setAttribute("jar", config.getJar(algo.infoid).getOrElse(""))
    command.setAttribute("pdioEvalJar", config.getJar("io.prediction.evaluations.scalding.itemrec").getOrElse(""))
    command.setAttribute("mahoutJar", config.getJar("io.prediction.algorithms.mahout").getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("algoid", algo.id)
    command.setAttribute("evalid", offlineEval.id)
    command.setAttribute("modelset", "false")
    command.setAttribute("hdfsRoot", config.settingsHdfsRoot)
    command.setAttribute("appdataTrainingDbType", config.appdataTrainingDbType)
    command.setAttribute("appdataTrainingDbName", config.appdataTrainingDbName)
    command.setAttribute("appdataTrainingDbHost", config.appdataTrainingDbHost)
    command.setAttribute("appdataTrainingDbPort", config.appdataTrainingDbPort)
    command.setAttribute("modeldataDbType", config.modeldataTrainingDbType)
    command.setAttribute("modeldataDbName", config.modeldataTrainingDbName)
    command.setAttribute("modeldataDbHost", config.modeldataTrainingDbHost)
    command.setAttribute("modeldataDbPort", config.modeldataTrainingDbPort)
    command.setAttribute("modeldatadir", ModelDataDir(
      config.settingsHdfsRoot,
      app.id,
      engine.id,
      algo.id,
      Some(offlineEval.id)
    ))
    command.setAttribute("mahoutTempDir", BaseDir.algoDir(config.settingsHdfsRoot+"mahout_temp/", app.id, engine.id, algo.id, Some(offlineEval.id)))
    command.setAttribute("algoDir", BaseDir.algoDir(config.settingsHdfsRoot, app.id, engine.id, algo.id, Some(offlineEval.id)))
    command.setAttribute("dataFilePrefix", DataFile(config.settingsHdfsRoot, app.id, engine.id, algo.id, Some(offlineEval.id), ""))
    command.setAttribute("algoFilePrefix", AlgoFile(config.settingsHdfsRoot, app.id, engine.id, algo.id, Some(offlineEval.id), ""))

    /** Add a job, then build a trigger for it.
      * This is necessary for updating any existing job,
      * and make sure the trigger will fire.
      */
    val offlineEvalTrainingJob = newJob(classOf[SeqNativeJob]) withIdentity(algo.id.toString, offlineEvalTrainingJobGroup) storeDurably(true) build()
    offlineEvalTrainingJob.getJobDataMap().put(
      NativeJob.PROP_COMMAND,
      command.toString
    )

    offlineEvalTrainingJob
  }

  def offlineEvalMetricJob(config: Config, app: App, engine: Engine, algo: Algo, offlineEval: OfflineEval, metric: OfflineEvalMetric): JobDetail = {
    val command = new StringTemplate(offlineEvalMetricCommands(engine.enginetype))
    command.setAttributes(algo.params)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttributes(metric.params)
    command.setAttribute("base", config.base)
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("goalParam", engine.settings("goal"))
    command.setAttribute("pdioEvalJar", config.getJar("io.prediction.evaluations.scalding.itemrec").getOrElse(""))
    command.setAttribute("topkJar", config.getJar("io.prediction.evaluations.itemrec.topkitems").getOrElse(""))
    command.setAttribute("configFile", Option(System.getProperty("config.file")).map(c => "-Dconfig.file="+c).getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("algoid", algo.id)
    command.setAttribute("evalid", offlineEval.id)
    command.setAttribute("metricid", metric.id)
    command.setAttribute("hdfsRoot", config.settingsHdfsRoot)
    command.setAttribute("settingsDbType", config.settingsDbType)
    command.setAttribute("settingsDbName", config.settingsDbName)
    command.setAttribute("settingsDbHost", config.settingsDbHost)
    command.setAttribute("settingsDbPort", config.settingsDbPort)
    command.setAttribute("appdataTrainingDbType", config.appdataTrainingDbType)
    command.setAttribute("appdataTrainingDbName", config.appdataTrainingDbName)
    command.setAttribute("appdataTrainingDbHost", config.appdataTrainingDbHost)
    command.setAttribute("appdataTrainingDbPort", config.appdataTrainingDbPort)
    command.setAttribute("appdataTestDbType", config.appdataTestDbType)
    command.setAttribute("appdataTestDbName", config.appdataTestDbName)
    command.setAttribute("appdataTestDbHost", config.appdataTestDbHost)
    command.setAttribute("appdataTestDbPort", config.appdataTestDbPort)
    command.setAttribute("modeldataDbType", config.modeldataTrainingDbType)
    command.setAttribute("modeldataDbName", config.modeldataTrainingDbName)
    command.setAttribute("modeldataDbHost", config.modeldataTrainingDbHost)
    command.setAttribute("modeldataDbPort", config.modeldataTrainingDbPort)
    command.setAttribute("modeldatadir", ModelDataDir(
      config.settingsHdfsRoot,
      app.id,
      engine.id,
      algo.id,
      Some(offlineEval.id)
    ))

    /** Add a job, then build a trigger for it.
      * This is necessary for updating any existing job,
      * and make sure the trigger will fire.
      */
    val offlineEvalMetricJob = newJob(classOf[SeqNativeJob]) withIdentity(algo.id.toString+"."+metric.id.toString, offlineEvalMetricJobGroup) storeDurably(true) build()
    Logger.info(s"Metric ID ${metric.id}: Setting command: ${command.toString}")
    offlineEvalMetricJob.getJobDataMap().put(
      NativeJob.PROP_COMMAND,
      command.toString
    )

    offlineEvalMetricJob
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
      Logger.info("Algo ID %d: Going to run %s".format(algo.id, command))
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

class OfflineEvalStartJob extends Job {
  def execute(context: JobExecutionContext) = {
    val jobDataMap = context.getMergedJobDataMap
    val evalid = jobDataMap.getInt("evalid")

    val offlineEvals = Scheduler.offlineEvals

    offlineEvals.get(evalid) map { offlineEval =>
      Logger.info("Starting offline evaluation for OfflineEval ID %d.".format(evalid))
      offlineEvals.update(offlineEval.copy(starttime = Some(DateTime.now)))
    }
  }
}

class PollOfflineEvalResultsJob extends Job {
  def execute(context: JobExecutionContext) = {
    val jobDataMap = context.getMergedJobDataMap
    val evalid = jobDataMap.getInt("evalid")
    val algoids = jobDataMap.getString("algoids").split(",") map { _.toInt }
    val metricids = jobDataMap.getString("metricids").split(",") map { _.toInt }

    val offlineEvals = Scheduler.offlineEvals
    val offlineEvalResults = Scheduler.offlineEvalResults

    var allResultsPresent = false

    while (!allResultsPresent) {
      allResultsPresent = true
      for (algoid <- algoids) {
        for (metricid <- metricids) {
          allResultsPresent = allResultsPresent && (offlineEvalResults.getByEvalidAndMetricidAndAlgoid(evalid, metricid, algoid) map { _ => true } getOrElse false)
        }
      }
      Thread.sleep(1)
    }

    offlineEvals.get(evalid) map { offlineEval =>
      Logger.info("All results have been found for OfflineEval ID %d, Algo IDs: %s, Metric IDs: %s. Marking offline evaluation as completed.".format(evalid, algoids.mkString(", "), metricids.mkString(", ")))
      offlineEvals.update(offlineEval.copy(endtime = Some(DateTime.now)))
    }
  }
}

@DisallowConcurrentExecution
class SeqNativeJob extends NativeJob
