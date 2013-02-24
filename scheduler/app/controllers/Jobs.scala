package io.prediction.scheduler

import io.prediction.commons._
import io.prediction.commons.filepath.ModelDataDir
import io.prediction.commons.settings.{Algo, App, Engine, OfflineEval, OfflineEvalMetric}

import com.github.nscala_time.time.Imports._
import org.clapper.scalasti.StringTemplate
import org.quartz.DisallowConcurrentExecution
import org.quartz.{Job, JobDetail, JobExecutionContext}
import org.quartz.JobBuilder.newJob
import org.quartz.jobs.NativeJob

import play.api.Logger

object Jobs {
  val algoJobGroup = "predictionio-algo"
  val algoPostProcessJobGroup = "predictionio-algo-postprocess"
  val offlineEvalJobGroup = "predictionio-offlineeval"
  val offlineEvalSplitJobGroup = "predictionio-offlineeval-split"
  val offlineEvalTrainingJobGroup = "predictionio-offlineeval-training"
  val offlineEvalMetricJobGroup   = "predictionio-offlineeval-metrics"
  val offlineEvalResultsJobGroup = "predictionio-offlineevalresults"
  val offlineEvalSplitCommands = Map(
    "itemrec" -> "hadoop jar $jar$ io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplit --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ $itypes$ --trainingsize $trainingsize$ --testsize $testsize$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$"
  )
  val offlineEvalMetricCommands = Map(
    "itemrec" -> (
      "hadoop jar $jar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType file --modeldata_dbName $modeldatadir$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$ && " +
      "hadoop jar $jar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$"
    )
  )

  def algoJob(settingsConfig: settings.Config, appdataConfig: appdata.Config, modeldataConfig: modeldata.Config, app: App, engine: Engine, algo: Algo, batchcommands: Seq[String]) = {
    /** Build command from template. */
    val command = new StringTemplate(batchcommands.mkString(" && "))
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
    val job = newJob(classOf[SeqNativeJob]) withIdentity(algo.id.toString, algoJobGroup) build()
    job.getJobDataMap().put(
      NativeJob.PROP_COMMAND,
      command.toString
    )
    job
  }

  def offlineEvalSplitJob(settingsConfig: settings.Config, appdataConfig: appdata.Config, app: App, engine: Engine, offlineEval: OfflineEval) = {
    /** Build command from template. */
    val command = new StringTemplate(offlineEvalSplitCommands(engine.enginetype))
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttribute("jar", settingsConfig.getJar("io.prediction.evaluations.scalding.itemrec.trainingtestsplit").get)
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

  def offlineEvalTrainingJob(settingsConfig: settings.Config, appdataConfig: appdata.Config, app: App, engine: Engine, algo: Algo, offlineEval: OfflineEval, offlineEvalCommands: Seq[String]): JobDetail = {
    val command = new StringTemplate(offlineEvalCommands.mkString(" && "))
    command.setAttributes(algo.params)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes" + it.mkString(" "))
    }

    /** Fill in settings values. */
    command.setAttribute("jar", settingsConfig.getJar(algo.pkgname).get)
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("algoid", algo.id)
    command.setAttribute("evalid", offlineEval.id)
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
    val offlineEvalTrainingJob = newJob(classOf[SeqNativeJob]) withIdentity(algo.id.toString, offlineEvalTrainingJobGroup) build()
    offlineEvalTrainingJob.getJobDataMap().put(
      NativeJob.PROP_COMMAND,
      command.toString
    )

    offlineEvalTrainingJob
  }

  def offlineEvalMetricJob(settingsConfig: settings.Config, appdataConfig: appdata.Config, app: App, engine: Engine, algo: Algo, offlineEval: OfflineEval, metric: OfflineEvalMetric): JobDetail = {
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
    val offlineEvalMetricJob = newJob(classOf[SeqNativeJob]) withIdentity(metric.id.toString, offlineEvalMetricJobGroup) build()
    offlineEvalMetricJob.getJobDataMap().put(
      NativeJob.PROP_COMMAND,
      command.toString
    )

    offlineEvalMetricJob
  }
}

class FlipModelSetJob extends Job {
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
