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
    "itemrec" -> "$hadoop$ jar $pdioJar$ io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplit --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ $itypes$ --trainingsize $trainingsize$ --testsize $testsize$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$"
  )
  val offlineEvalMetricCommands = Map(
    "itemrec" -> (
      "$hadoop$ jar $pdioJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataDbType$ --modeldata_dbName $modeldataDbName$ --modeldata_dbHost $modeldataDbHost$ --modeldata_dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$ && " +
      "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$ && " +
      "$hadoop$ jar $pdioJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$"
    )
  )

  def algoJobs(settingsConfig: settings.Config, appdataConfig: appdata.Config, modeldataConfig: modeldata.Config, app: App, engine: Engine, algo: Algo, batchcommands: Seq[String]) = {
    /** Build command from template. */
    val command = new StringTemplate(batchcommands.mkString(" && "))
    command.setAttributes(algo.params)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttribute("base", Option(System.getProperty("io.prediction.base")).getOrElse(".."))
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("jar", settingsConfig.getJar(algo.infoid).getOrElse(""))
    command.setAttribute("pdioJar", settingsConfig.getJar("io.prediction.algorithms.scalding").getOrElse(""))
    command.setAttribute("mahoutJar", settingsConfig.getJar("io.prediction.algorithms.mahout").getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("algoid", algo.id)
    command.setAttribute("modelset", "$modelset$")
    command.setAttribute("hdfsRoot", settingsConfig.settingsHdfsRoot)
    command.setAttribute("appdataDbType", appdataConfig.appdataDbType)
    command.setAttribute("appdataDbName", appdataConfig.appdataDbName)
    command.setAttribute("appdataDbHost", appdataConfig.appdataDbHost)
    command.setAttribute("appdataDbPort", appdataConfig.appdataDbPort)
    command.setAttribute("modeldataDbType", modeldataConfig.modeldataDbType)
    command.setAttribute("modeldataDbName", modeldataConfig.modeldataDbName)
    command.setAttribute("modeldataDbHost", modeldataConfig.modeldataDbHost)
    command.setAttribute("modeldataDbPort", modeldataConfig.modeldataDbPort)
    command.setAttribute("mahoutTempDir", BaseDir.algoDir(settingsConfig.settingsHdfsRoot+"mahout_temp/", app.id, engine.id, algo.id, None))
    command.setAttribute("algoDir", BaseDir.algoDir(settingsConfig.settingsHdfsRoot, app.id, engine.id, algo.id, None))
    command.setAttribute("dataFilePrefix", DataFile(settingsConfig.settingsHdfsRoot, app.id, engine.id, algo.id, None, ""))
    command.setAttribute("algoFilePrefix", AlgoFile(settingsConfig.settingsHdfsRoot, app.id, engine.id, algo.id, None, ""))

    /** Add a job, then build a trigger for it.
      * This is necessary for updating any existing job,
      * and make sure the trigger will fire.
      */
    val job = newJob(classOf[AlgoJob]) withIdentity(algo.id.toString, algoJobGroup) build()
    job.getJobDataMap().put("template", command.toString)
    job.getJobDataMap().put("algoid", algo.id)
    job.getJobDataMap().put("enginetype", engine.enginetype)

    job
  }

  def offlineEvalSplitJob(settingsConfig: settings.Config, appdataConfig: appdata.Config, app: App, engine: Engine, offlineEval: OfflineEval) = {
    /** Build command from template. */
    val command = new StringTemplate(offlineEvalSplitCommands(engine.enginetype))
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttribute("base", Option(System.getProperty("io.prediction.base")).getOrElse(".."))
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("pdioJar", settingsConfig.getJar("io.prediction.algorithms.scalding").getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("evalid", offlineEval.id)
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

    val offlineEvalSplitJob = newJob(classOf[SeqNativeJob]) withIdentity(offlineEval.id.toString, offlineEvalSplitJobGroup) build()
    offlineEvalSplitJob.getJobDataMap().put(
      NativeJob.PROP_COMMAND,
      command.toString
    )

    offlineEvalSplitJob
  }

  def offlineEvalTrainingJob(settingsConfig: settings.Config, appdataConfig: appdata.Config, modeldataTrainingSetConfig: modeldata.TrainingSetConfig, app: App, engine: Engine, algo: Algo, offlineEval: OfflineEval, offlineEvalCommands: Seq[String]): JobDetail = {
    val command = new StringTemplate(offlineEvalCommands.mkString(" && "))
    command.setAttributes(algo.params)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttribute("base", Option(System.getProperty("io.prediction.base")).getOrElse(".."))
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("jar", settingsConfig.getJar(algo.infoid).getOrElse(""))
    command.setAttribute("pdioJar", settingsConfig.getJar("io.prediction.algorithms.scalding").getOrElse(""))
    command.setAttribute("mahoutJar", settingsConfig.getJar("io.prediction.algorithms.mahout").getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("algoid", algo.id)
    command.setAttribute("evalid", offlineEval.id)
    command.setAttribute("modelset", "false")
    command.setAttribute("hdfsRoot", settingsConfig.settingsHdfsRoot)
    command.setAttribute("appdataTrainingDbType", appdataConfig.appdataTrainingDbType)
    command.setAttribute("appdataTrainingDbName", appdataConfig.appdataTrainingDbName)
    command.setAttribute("appdataTrainingDbHost", appdataConfig.appdataTrainingDbHost)
    command.setAttribute("appdataTrainingDbPort", appdataConfig.appdataTrainingDbPort)
    command.setAttribute("modeldataDbType", modeldataTrainingSetConfig.modeldataDbType)
    command.setAttribute("modeldataDbName", modeldataTrainingSetConfig.modeldataDbName)
    command.setAttribute("modeldataDbHost", modeldataTrainingSetConfig.modeldataDbHost)
    command.setAttribute("modeldataDbPort", modeldataTrainingSetConfig.modeldataDbPort)
    command.setAttribute("modeldatadir", ModelDataDir(
      settingsConfig.settingsHdfsRoot,
      app.id,
      engine.id,
      algo.id,
      Some(offlineEval.id)
    ))
    command.setAttribute("mahoutTempDir", BaseDir.algoDir(settingsConfig.settingsHdfsRoot+"mahout_temp/", app.id, engine.id, algo.id, Some(offlineEval.id)))
    command.setAttribute("algoDir", BaseDir.algoDir(settingsConfig.settingsHdfsRoot, app.id, engine.id, algo.id, Some(offlineEval.id)))
    command.setAttribute("dataFilePrefix", DataFile(settingsConfig.settingsHdfsRoot, app.id, engine.id, algo.id, Some(offlineEval.id), ""))
    command.setAttribute("algoFilePrefix", AlgoFile(settingsConfig.settingsHdfsRoot, app.id, engine.id, algo.id, Some(offlineEval.id), ""))

    /** Add a job, then build a trigger for it.
      * This is necessary for updating any existing job,
      * and make sure the trigger will fire.
      */
    val offlineEvalTrainingJob = newJob(classOf[SeqNativeJob]) withIdentity(algo.id.toString, offlineEvalTrainingJobGroup) build()
    offlineEvalTrainingJob.getJobDataMap().put(
      NativeJob.PROP_COMMAND,
      command.toString
    )

    offlineEvalTrainingJob
  }

  def offlineEvalMetricJob(settingsConfig: settings.Config, appdataConfig: appdata.Config, modeldataTrainingSetConfig: modeldata.TrainingSetConfig, app: App, engine: Engine, algo: Algo, offlineEval: OfflineEval, metric: OfflineEvalMetric): JobDetail = {
    val command = new StringTemplate(offlineEvalMetricCommands(engine.enginetype))
    command.setAttributes(algo.params)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttributes(metric.params)
    command.setAttribute("base", Option(System.getProperty("io.prediction.base")).getOrElse(".."))
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("goalParam", engine.settings("goal"))
    command.setAttribute("pdioJar", settingsConfig.getJar("io.prediction.algorithms.scalding").getOrElse(""))
    command.setAttribute("topkJar", settingsConfig.getJar("io.prediction.evaluations.itemrec.topkitems").getOrElse(""))
    command.setAttribute("configFile", Option(System.getProperty("config.file")).map(c => "-Dconfig.file="+c).getOrElse(""))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("algoid", algo.id)
    command.setAttribute("evalid", offlineEval.id)
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
    command.setAttribute("modeldataDbType", modeldataTrainingSetConfig.modeldataDbType)
    command.setAttribute("modeldataDbName", modeldataTrainingSetConfig.modeldataDbName)
    command.setAttribute("modeldataDbHost", modeldataTrainingSetConfig.modeldataDbHost)
    command.setAttribute("modeldataDbPort", modeldataTrainingSetConfig.modeldataDbPort)
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
    val offlineEvalMetricJob = newJob(classOf[SeqNativeJob]) withIdentity(algo.id.toString+"."+metric.id.toString, offlineEvalMetricJobGroup) build()
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
    val config = new settings.Config
    val algos = config.getAlgos
    val modeldataConfig = new modeldata.Config
    val itemRecScores = modeldataConfig.getItemRecScores
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

    val settingsConfig = new settings.Config
    val offlineEvals = settingsConfig.getOfflineEvals

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

    val settingsConfig = new settings.Config
    val offlineEvals = settingsConfig.getOfflineEvals
    val offlineEvalResults = settingsConfig.getOfflineEvalResults

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
