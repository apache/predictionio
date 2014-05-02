package io.prediction.scheduler

import io.prediction.commons._
import io.prediction.commons.filepath._
import io.prediction.commons.settings.{ Algo, App, Engine, OfflineEval, OfflineEvalMetric, OfflineTune }

import com.github.nscala_time.time.Imports._
import org.clapper.scalasti.StringTemplate
import org.quartz.{ DisallowConcurrentExecution, PersistJobDataAfterExecution }
import org.quartz.{ InterruptableJob, Job, JobDetail, JobExecutionContext }
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey.jobKey
import org.quartz.jobs.NativeJob

import play.api.Logger

import scala.collection.mutable.{ HashMap, Map, SynchronizedMap }
import scala.concurrent.future
import scala.sys.process._

import Contexts.stepExecutionContext

object Jobs {
  val algoJobGroup = "predictionio-algo"
  val offlineEvalJobGroup = "predictionio-offlineeval"
  val offlineTuneJobGroup = "predictionio-offlinetune"

  def algoJob(config: Config, app: App, engine: Engine, algo: Algo, batchcommands: Seq[String]) = {
    /**
     * Add a job, then build a trigger for it.
     * This is necessary for updating any existing job,
     * and make sure the trigger will fire.
     */
    val job = newJob(classOf[AlgoJob]) withIdentity (algo.id.toString, algoJobGroup) storeDurably (true) build ()
    job.getJobDataMap().put("algoid", algo.id)
    job.getJobDataMap().put("engineinfoid", engine.infoid)

    job
  }

  /**
   * Offline Evaluation Flow
   *
   * 1. Iterate the following for a specified number of times
   *    1. Perform data splitting
   *    2. For each algo to be evaluated
   *       1. Run algo on training set
   *       2. Run all metrics on model data from the above against test set
   * 2. Mark offline evaluation as finished
   */
  def offlineEvalJob(config: Config, app: App, engine: Engine, offlineEval: OfflineEval) = {
    /**
     * Add a job, then build a trigger for it.
     * This is necessary for updating any existing job,
     * and make sure the trigger will fire.
     */
    val job = newJob(classOf[OfflineEvalJob]) withIdentity (offlineEval.id.toString, offlineEvalJobGroup) storeDurably (true) build ()
    job.getJobDataMap().put("evalid", offlineEval.id)
    job
  }

  /**
   * Offline Tuning Flow
   *
   * 1. Perform multiple set of data splitting
   * 2. Train and evaluate baseline algo against data sets from 1.
   * 3. For a specified number of iterations:
   *    1. Parameter generator generates new parameters based on previous evaluation results.
   *    2. Train algo and run metrics against both validation and test sets.
   * 4. Mark offline evaluation as finished
   */
  def offlineTuneJob(config: Config, app: App, engine: Engine, offlineTune: OfflineTune) = {
    /**
     * Add a job, then build a trigger for it.
     * This is necessary for updating any existing job,
     * and make sure the trigger will fire.
     */
    val job = newJob(classOf[OfflineTuneJob]) withIdentity (offlineTune.id.toString, offlineTuneJobGroup) storeDurably (true) build ()
    job.getJobDataMap().put("tuneid", offlineTune.id)
    job
  }

  def setSharedAttributes(command: StringTemplate, config: Config, app: App,
    engine: Engine, algo: Option[Algo], offlineEval: Option[OfflineEval],
    metric: Option[OfflineEvalMetric],
    params: Option[collection.immutable.Map[String, Any]] = None) = {
    /** Custom attributes */
    params map { command.setAttributes(_) }

    /** OfflineEvalMetric-specific attributes */
    metric map { met =>
      command.setAttributes(command.attributes ++ met.params)
      command.setAttribute("metricid", met.id)
      command.attributes.get("iteration").getOrElse(command.setAttribute("iteration", 0))
      command.attributes.get("splitset").getOrElse(command.setAttribute("splitset", "test"))
    }

    /** OfflineEval-specific attributes */
    offlineEval map { oe =>
      command.setAttribute("evalid", oe.id)
      command.setAttribute("modelset", "false")
    }

    /** Algo-specific attributes */
    algo map { alg =>
      val defaultParams = Scheduler.algoInfos.get(alg.infoid) map { _.params.mapValues(_.defaultvalue) } getOrElse Map[String, String]()
      command.setAttributes(command.attributes ++ defaultParams ++ alg.params)
      command.setAttribute("algoid", alg.id)
      command.setAttribute("localTempDir", BaseDir.algoDir(config.settingsLocalTempRoot, app.id, engine.id, alg.id, offlineEval.map(_.id)))
      command.setAttribute("mahoutTempDir", BaseDir.algoDir(config.settingsHdfsRoot + "mahout_temp/", app.id, engine.id, alg.id, offlineEval.map(_.id)))
      command.setAttribute("algoDir", BaseDir.algoDir(config.settingsHdfsRoot, app.id, engine.id, alg.id, offlineEval.map(_.id)))
      command.setAttribute("dataFilePrefix", DataFile(config.settingsHdfsRoot, app.id, engine.id, alg.id, offlineEval.map(_.id), ""))
      command.setAttribute("algoFilePrefix", AlgoFile(config.settingsHdfsRoot, app.id, engine.id, alg.id, offlineEval.map(_.id), ""))
    }

    /** Engine-specific attributes */
    val engineDefaultParams = Scheduler.engineInfos.get(engine.infoid) map {
      _.params.mapValues(_.defaultvalue)
    } getOrElse Map[String, String]()
    command.setAttributes(command.attributes ++ engineDefaultParams ++ engine.params)

    /** Common attributes */
    val appdataItems = config.getAppdataItems
    command.setAttribute("base", config.base)
    command.setAttribute("hadoop", Scheduler.hadoopCommand)
    command.setAttribute("itemCount", appdataItems.countByAppid(app.id))

    /**
     * Locate JAR names
     * Use those from config file first, then override with SystemInfos.
     */
    config.jars foreach { kv => command.setAttribute(kv._1, kv._2) }
    val systemInfosJarsR = """^jars\.(.*)""".r
    config.getSettingsSystemInfos.getAll foreach { e =>
      systemInfosJarsR findFirstIn e.id match {
        case Some(systemInfosJarsR(jarKey)) => command.setAttribute(jarKey, e.value)
        case None => Unit
      }
    }

    command.setAttribute("configFile", Option(System.getProperty("config.file")).map(c => "-Dconfig.file=" + c).getOrElse("-Dconfig.file=conf/application.conf"))
    command.setAttribute("appid", app.id)
    command.setAttribute("engineid", engine.id)
    command.setAttribute("hdfsRoot", config.settingsHdfsRoot)
    command.setAttribute("hadoopOptions", Seq(
      config.schedulerMapredMinSplitSize map { x => s"-Dmapred.min.split.size=${x}" } getOrElse "",
      config.schedulerMapredMapTasks map { x => s"-Dmapred.map.tasks=${x}" } getOrElse "",
      config.schedulerMapredReduceTasks map { x => s"-Dmapred.reduce.tasks=${x}" } getOrElse "").mkString(" "))
    command.setAttribute("localTempRoot", config.settingsLocalTempRoot)
    command.setAttribute("javaOpts", config.schedulerChildJavaOpts)
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
    command.setAttribute("appdataValidationDbType", config.appdataValidationDbType)
    command.setAttribute("appdataValidationDbName", config.appdataValidationDbName)
    command.setAttribute("appdataValidationDbHost", config.appdataValidationDbHost)
    command.setAttribute("appdataValidationDbPort", config.appdataValidationDbPort)
    command.attributes.get("appdataTestDbType").getOrElse(command.setAttribute("appdataTestDbType", config.appdataTestDbType))
    command.attributes.get("appdataTestDbName").getOrElse(command.setAttribute("appdataTestDbName", config.appdataTestDbName))
    command.attributes.get("appdataTestDbHost").getOrElse(command.setAttribute("appdataTestDbHost", config.appdataTestDbHost))
    command.attributes.get("appdataTestDbPort").getOrElse(command.setAttribute("appdataTestDbPort", config.appdataTestDbPort))
    command.setAttribute("modeldataDbType", config.modeldataDbType)
    command.setAttribute("modeldataDbName", config.modeldataDbName)
    command.setAttribute("modeldataDbHost", config.modeldataDbHost)
    command.setAttribute("modeldataDbPort", config.modeldataDbPort)
    command.setAttribute("modeldataTrainingDbType", config.modeldataTrainingDbType)
    command.setAttribute("modeldataTrainingDbName", config.modeldataTrainingDbName)
    command.setAttribute("modeldataTrainingDbHost", config.modeldataTrainingDbHost)
    command.setAttribute("modeldataTrainingDbPort", config.modeldataTrainingDbPort)
    engine.itypes foreach { it =>
      command.setAttribute("itypes", "--itypes " + it.mkString(" ")) // NOTE: a space ' ' is necessary after --itypes
      command.setAttribute("itypesCSV", "--itypes " + it.mkString(","))
    }
    command.setAttribute("unseenOnly", engine.params.getOrElse("unseenonly", false))
    command.setAttribute("recommendationTime", System.currentTimeMillis)
  }
}

class UpdateCheckJob extends Job {
  override def execute(context: JobExecutionContext) = {
    Logger.info("Checking for new release...")
    "bin/updatecheck --answer n".!
  }
}

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
class AlgoJob extends InterruptableJob {
  @volatile
  var kill = false

  var exitCode: Int = 0
  var finishFlag: Boolean = false

  @volatile
  var proc: Option[Process] = None

  override def execute(context: JobExecutionContext) = {
    val jobDataMap = context.getMergedJobDataMap
    val algoid = jobDataMap.getInt("algoid")
    val engineinfoid = jobDataMap.getString("engineinfoid")
    val config = Scheduler.config
    val apps = Scheduler.apps
    val engines = Scheduler.engines
    val algos = Scheduler.algos
    val algoInfos = Scheduler.algoInfos
    val logPrefix = s"Algo ID ${algoid}: "

    algos.get(algoid) map { algo =>
      engines.get(algo.engineid) map { engine =>
        apps.get(engine.appid) map { app =>
          algoInfos.get(algo.infoid) map { info =>
            info.batchcommands map { batchcommands =>
              Logger.info(s"${logPrefix}Current model set is ${algo.modelset}")
              Logger.info(s"${logPrefix}Running database specific before-logic for model set ${!algo.modelset}")
              val modelData = config.getModeldata(engine.infoid)
              modelData.before(algo.id, !algo.modelset)
              Logger.info(s"${logPrefix}Launching algo job for model set ${!algo.modelset}")
              val commands = batchcommands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, Some(algo), None, None, Some(collection.immutable.Map("modelset" -> !algo.modelset))).toString }

              commands map { _.trim } foreach { c =>
                this.synchronized {
                  if (!kill && !c.isEmpty && exitCode == 0) {
                    Logger.info(s"${logPrefix}Going to run: $c")
                    proc = Some(Process(c, None, "JVM_OPT" -> config.schedulerChildJavaOpts).run)
                    Logger.info(s"${logPrefix}Scheduler waiting for sub-process to finish")
                  }
                }

                if (!kill && !c.isEmpty && exitCode == 0) {
                  exitCode = proc.get.exitValue
                  Logger.info(s"${logPrefix}Sub-process has finished with exit code ${exitCode}")
                }
              }

              finishFlag = true

              /** Display completion information */
              if (kill) {
                Logger.info(s"${logPrefix}Sub-process was killed upon request")
                Logger.info(s"${logPrefix}Not flipping model set flag because the algo job was killed")
              } else if (exitCode == 0) {
                Logger.info(s"${logPrefix}Running database specific after-logic for model set ${!algo.modelset}")
                modelData.after(algo.id, !algo.modelset)
                Logger.info(s"${logPrefix}Flipping model set flag to ${!algo.modelset}")
                algos.update(algo.copy(modelset = !algo.modelset, lasttraintime = Some(DateTime.now)))
                Logger.info(s"${logPrefix}Running database specific deletion for model set ${algo.modelset}")
                modelData.delete(algo.id, algo.modelset)
                Logger.info(s"${logPrefix}Job completed")
              } else {
                Logger.warn(s"${logPrefix}Not flipping model set flag because the algo job returned non-zero exit code")
              }
            } getOrElse {
              Logger.warn(s"${logPrefix}Not starting algorithm training because this algorithm has no command")
            }
          } getOrElse {
            Logger.warn(s"${logPrefix}Not starting algorithm training because information of this algorithm cannot be found from the database")
          }
        } getOrElse {
          Logger.warn(s"${logPrefix}Not starting algorithm training because the app that owns this algorithm cannot be found from the database")
        }
      } getOrElse {
        Logger.warn(s"${logPrefix}Not starting algorithm training because the engine that owns this algorithm cannot be found from the database")
      }
    } getOrElse {
      Logger.warn(s"${logPrefix}Not starting algorithm training because the algorithm cannot be found from the database")
    }
  }

  override def interrupt() = {
    this.synchronized {
      kill = true
      proc map { _.destroy }
    }
  }
}

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
class OfflineEvalJob extends InterruptableJob {
  @volatile
  var kill = false

  val exitCodes: Map[String, Int] = new HashMap[String, Int] with SynchronizedMap[String, Int]
  val finishFlags: Map[String, Boolean] = new HashMap[String, Boolean] with SynchronizedMap[String, Boolean]
  val procs: Map[String, Process] = new HashMap[String, Process] with SynchronizedMap[String, Process]

  def step(evalid: Int, iteration: Int, steptype: String, commands: Seq[String], algoid: Option[Int] = None, metricid: Option[Int] = None, algoids: Option[Seq[Int]] = None, metricids: Option[Seq[Int]] = None) = future {
    val logPrefix = s"OfflineEval ID $evalid: Iteration ${iteration}: " + algoid.map(id => s"Algo ID ${id}: ").getOrElse("") + metricid.map(id => s"Metric ID ${id}: ").getOrElse("")
    val key = s"${steptype}-${iteration}" + algoid.map(id => s"-${id}").getOrElse("") + metricid.map(id => s"-${id}").getOrElse("")
    var abort = false

    Some(steptype) collect {
      case "split" => {
        if (iteration > 1) {
          val iterationkey = s"iteration-${iteration - 1}"
          while (!finishFlags(iterationkey)) {
            Thread.sleep(1000)
          }
        }

        /** Delete old model data, if any (for recovering from an incomplete run, and clean old score for multi-iterations) */
        Scheduler.offlineEvals.get(evalid) map { offlineEval =>
          Scheduler.engines.get(offlineEval.engineid) map { engine =>
            val algosToRun = Scheduler.algos.getByOfflineEvalid(offlineEval.id).toSeq
            val modelData = Scheduler.config.getModeldataTraining(engine.infoid)
            algosToRun foreach { algo =>
              Logger.info(s"${logPrefix}Algo ID ${algo.id}: Deleting any old model data")
              modelData.delete(algo.id, false)
            }
            Logger.info(s"${logPrefix}Deleting any old user-to-item actions")
            Scheduler.appdataTrainingU2IActions.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestU2IActions.deleteByAppid(offlineEval.id)
          }
        }
      }
      case "training" => {
        val splitkey = s"split-${iteration}"
        val trainingkey = s"training-${iteration}-${algoid.get}"
        while (!finishFlags(splitkey)) {
          Thread.sleep(1000)
        }
        if (exitCodes(splitkey) != 0) {
          abort = true
          exitCodes(trainingkey) = 1
          Logger.info(s"${logPrefix}(${steptype}) Aborted due to split error")
        }
      }
      case "metric" => {
        val trainingkey = s"training-${iteration}-${algoid.get}"
        val metrickey = s"metric-${iteration}-${algoid.get}-${metricid.get}"
        while (!finishFlags(trainingkey)) {
          Thread.sleep(1000)
        }
        if (exitCodes(trainingkey) != 0) {
          abort = true
          exitCodes(metrickey) = 1
          Logger.info(s"${logPrefix}(${steptype}) Aborted due to training error")
        }
      }
      case "iteration" => {
        val keys = for {
          aid <- algoids.get
          mid <- metricids.get
        } yield s"metric-${iteration}-${aid}-${mid}"

        while (!finishFlags.filterKeys(keys.contains(_)).values.reduce((a, b) => a && b)) {
          Thread.sleep(1000)
        }

        Logger.info(s"${logPrefix}(${steptype}) Finished iteration")
      }
    }

    commands map { _.trim } foreach { c =>
      var exception = false
      this.synchronized {
        if (!kill && !abort && !c.isEmpty && exitCodes(key) == 0) {
          Logger.info(s"${logPrefix}(${steptype}) Going to run: $c")
          try {
            procs(key) = Process(c).run
            Logger.info(s"${logPrefix}(${steptype}) Scheduler waiting for sub-process to finish")
          } catch {
            case e: java.io.IOException => {
              exception = true
              Logger.info(s"${logPrefix}(${steptype}) ${e.getMessage}")
            }
          }
        }
      }

      // Continue if the last command succeeded
      if (exitCodes(key) == 0) {
        procs.get(key) map { p =>
          val exitCode = if (exception) 1 else p.exitValue

          /** Save completion information for global access */
          exitCodes(key) = exitCode

          if (exception)
            Logger.info(s"${logPrefix}(${steptype}) Exception trying to run sub-process")
          else
            Logger.info(s"${logPrefix}(${steptype}) Sub-process has finished with exit code ${exitCode}")
        }
      }
    }

    finishFlags(key) = true

    /** Display completion information */
    if (kill) Logger.info(s"${logPrefix}(${steptype}) Sub-process was killed upon request")
  }

  override def execute(context: JobExecutionContext): Unit = {
    val jobDataMap = context.getMergedJobDataMap
    val evalid = jobDataMap.getInt("evalid")

    val config = Scheduler.config
    val apps = Scheduler.apps
    val engines = Scheduler.engines
    val algos = Scheduler.algos
    val algoInfos = Scheduler.algoInfos
    val offlineEvals = Scheduler.offlineEvals
    val offlineEvalSplitters = Scheduler.offlineEvalSplitters
    val offlineEvalSplitterInfos = Scheduler.offlineEvalSplitterInfos
    val offlineEvalMetrics = Scheduler.offlineEvalMetrics
    val offlineEvalMetricInfos = Scheduler.offlineEvalMetricInfos

    val logPrefix = s"OfflineEval ID $evalid: "

    offlineEvals.get(evalid) map { offlineEval =>
      engines.get(offlineEval.engineid) map { engine =>
        apps.get(engine.appid) map { app =>

          val totalIterations = offlineEval.iterations
          val splittersToRun = offlineEvalSplitters.getByEvalid(offlineEval.id).toSeq
          val algosToRun = algos.getByOfflineEvalid(offlineEval.id).toSeq
          val metricsToRun = offlineEvalMetrics.getByEvalid(offlineEval.id).toSeq

          val algoids = algosToRun map { _.id }
          val metricids = metricsToRun map { _.id }

          Logger.info(s"${logPrefix}Starting offline evaluation with ${totalIterations} iteration(s)")

          /** Mark the start time */
          val offlineEvalWithStartTime = offlineEval.copy(starttime = Some(DateTime.now))
          offlineEvals.update(offlineEvalWithStartTime)

          for (currentIteration <- 1 to totalIterations) {
            val iterationParam = collection.immutable.Map("iteration" -> currentIteration)

            /** Spiltters setup (support 1 splitter for now) */
            if (splittersToRun.length > 0) {
              val splitkey = s"split-${currentIteration}"
              exitCodes(splitkey) = 0
              finishFlags(splitkey) = false

              val splitter = splittersToRun(0)
              offlineEvalSplitterInfos.get(splitter.infoid) map { splitterInfo =>
                splitterInfo.commands map { commands =>
                  val splitterCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, None, Some(offlineEval), None, Some(splitterInfo.params.mapValues(_.defaultvalue) ++ splitter.settings ++ iterationParam)).toString }
                  step(evalid, currentIteration, "split", splitterCommands)
                } getOrElse {
                  Logger.warn(s"${logPrefix}Not doing data split because splitter information for ${splitter.infoid} contains no command")
                  step(evalid, currentIteration, "split", Seq())
                }
              } getOrElse {
                Logger.warn(s"${logPrefix}Not doing data split because splitter information for ${splitter.infoid} is missing")
                step(evalid, currentIteration, "split", Seq())
              }
            }

            /** Training and metric setup */
            algosToRun foreach { algo =>
              val trainingkey = s"training-${currentIteration}-${algo.id}"
              exitCodes(trainingkey) = 0
              finishFlags(trainingkey) = false

              algoInfos.get(algo.infoid) map { algoInfo =>
                algoInfo.offlineevalcommands map { commands =>
                  val trainingCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, Some(algo), Some(offlineEval), None, Some(iterationParam)).toString }
                  step(evalid, currentIteration, "training", trainingCommands, Some(algo.id))
                } getOrElse {
                  Logger.warn(s"${logPrefix}Algo ID ${algo.id}: Not doing training because algo information for ${algo.infoid} contains no command for offline evaluation")
                  step(evalid, currentIteration, "training", Seq(), Some(algo.id))
                }
              } getOrElse {
                Logger.warn(s"${logPrefix}Algo ID ${algo.id}: Not doing training because algo information for ${algo.infoid} is missing")
                step(evalid, currentIteration, "training", Seq(), Some(algo.id))
              }

              /** Run metrics */
              metricsToRun foreach { metric =>
                val metrickey = s"metric-${currentIteration}-${algo.id}-${metric.id}"
                exitCodes(metrickey) = 0
                finishFlags(metrickey) = false

                offlineEvalMetricInfos.get(metric.infoid) map { metricInfo =>
                  metricInfo.commands map { commands =>
                    val metricCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, Some(algo), Some(offlineEval), Some(metric), Some(iterationParam)).toString }
                    step(evalid, currentIteration, "metric", metricCommands, Some(algo.id), Some(metric.id))
                  } getOrElse {
                    Logger.warn(s"${logPrefix}Algo ID ${algo.id}: Metric ID ${metric.id}: Not doing training because algo information for ${algo.infoid} contains no command for offline evaluation")
                    step(evalid, currentIteration, "metric", Seq(), Some(algo.id), Some(metric.id))
                  }
                } getOrElse {
                  Logger.warn(s"${logPrefix}Algo ID ${algo.id}: Metric ID ${metric.id}: Not doing training because algo information for ${algo.infoid} is missing")
                  step(evalid, currentIteration, "metric", Seq(), Some(algo.id), Some(metric.id))
                }
              }
            }

            val iterationkey = s"iteration-${currentIteration}"
            exitCodes(iterationkey) = 0
            finishFlags(iterationkey) = false

            step(evalid, currentIteration, "iteration", Seq(), None, None, Some(algoids), Some(metricids))
          }

          /** Block on the last iteration */
          while (!finishFlags(s"iteration-${totalIterations}")) {
            Thread.sleep(1000)
          }

          /** Clean up if ended normally or killed */
          val sumExitCodes = exitCodes.values.sum
          if (kill || sumExitCodes == 0) {
            val modelData = config.getModeldataTraining(engine.infoid)
            algosToRun foreach { algo =>
              Logger.info(s"${logPrefix}Algo ID ${algo.id}: Deleting used model data")
              modelData.delete(algo.id, false)
            }
            Logger.info(s"${logPrefix}Deleting used app data")
            Scheduler.appdataTrainingUsers.deleteByAppid(offlineEval.id)
            Scheduler.appdataTrainingItems.deleteByAppid(offlineEval.id)
            Scheduler.appdataTrainingU2IActions.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestUsers.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestItems.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestU2IActions.deleteByAppid(offlineEval.id)
          }

          /** Check for errors from metric */
          Logger.info(s"${logPrefix}Exit code summary:")

          for (currentIteration <- 1 to totalIterations) {
            Logger.info(s"${logPrefix}Iteration ${currentIteration}:")
            Logger.info(s"${logPrefix}  Split: " + exitCodes(s"split-${currentIteration}"))
            algoids foreach { algoid =>
              Logger.info(s"${logPrefix}  Algo ID ${algoid}: " + exitCodes(s"training-${currentIteration}-${algoid}"))
              metricids foreach { metricid =>
                Logger.info(s"${logPrefix}    Metric ID ${metricid}: " + exitCodes(s"metric-${currentIteration}-${algoid}-${metricid}"))
              }
            }
          }

          if (sumExitCodes != 0)
            Logger.warn(s"${logPrefix}Offline evaluation completed with error(s)")
          else
            Logger.info(s"${logPrefix}Offline evaluation completed")

          /** Mark the end time since this is used to determine whether the run has finished */
          offlineEvals.update(offlineEvalWithStartTime.copy(endtime = Some(DateTime.now)))

        } getOrElse {
          Logger.warn(s"${logPrefix}Not starting offline evaluation because the app that owns this offline evaluation cannot be found from the database")
        }
      } getOrElse {
        Logger.warn(s"${logPrefix}Not starting offline evaluation because the engine that owns this offline evaluation cannot be found from the database")
      }
    } getOrElse {
      Logger.warn(s"${logPrefix}Not starting offline evaluation because the offline evaluation cannot be found from the database")
    }
  }

  override def interrupt() = {
    this.synchronized {
      kill = true
      procs.values map { _.destroy }
    }
  }
}

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
class OfflineTuneJob extends InterruptableJob {
  @volatile
  var kill = false

  val exitCodes: Map[String, Int] = new HashMap[String, Int] with SynchronizedMap[String, Int]
  val finishFlags: Map[String, Boolean] = new HashMap[String, Boolean] with SynchronizedMap[String, Boolean]
  val procs: Map[String, Process] = new HashMap[String, Process] with SynchronizedMap[String, Process]

  def step(tuneid: Int, evalid: Int, iteration: Int, steptype: String, commands: Seq[String], algoid: Option[Int] = None, metricid: Option[Int] = None, algoids: Option[Seq[Int]] = None, metricids: Option[Seq[Int]] = None) = future {
    val logPrefix = s"OfflineTune ID $tuneid: OfflineEval ID $evalid: Iteration ${iteration}: " + algoid.map(id => s"Algo ID ${id}: ").getOrElse("") + metricid.map(id => s"Metric ID ${id}: ").getOrElse("")
    val key = s"${steptype}-${evalid}-${iteration}" + algoid.map(id => s"-${id}").getOrElse("") + metricid.map(id => s"-${id}").getOrElse("")
    var abort = false

    steptype match {
      case "split" => {
        /**
         * if (iteration > 1) {
         * val iterationkey = s"iteration-${iteration-1}"
         * while (!finishFlags(iterationkey)) {
         * Thread.sleep(1000)
         * }
         * }
         */
      }
      case "paramgen" => {
        val keys = Scheduler.offlineEvals.getByTuneid(tuneid).toSeq.map(oe => s"iteration-${oe.id}-${iteration - 1}")
        while (!finishFlags.filterKeys(keys.contains(_)).values.reduce((a, b) => a && b)) {
          Thread.sleep(1000)
        }
      }
      case "training" => {
        if (iteration == 0) {
          val splitkey = s"split-${evalid}-${iteration}"
          while (!finishFlags(splitkey)) {
            Thread.sleep(1000)
          }
          if (exitCodes(splitkey) != 0) {
            abort = true
            Logger.info(s"${logPrefix}(${steptype}) Aborted due to split error")
          }
        }
      }
      case "metric" => {
        val trainingkey = s"training-${evalid}-${iteration}-${algoid.get}"
        while (!finishFlags(trainingkey)) {
          Thread.sleep(1000)
        }
        if (exitCodes(trainingkey) != 0) {
          abort = true
          Logger.info(s"${logPrefix}(${steptype}) Aborted due to training error")
        }
      }
      case "iteration" => {
        val keys = for {
          aid <- algoids.get
          mid <- metricids.get
        } yield s"metric-${evalid}-${iteration}-${aid}-${mid}"

        while (!finishFlags.filterKeys(keys.contains(_)).values.reduce((a, b) => a && b)) {
          Thread.sleep(1000)
        }

        Logger.info(s"${logPrefix}(${steptype}) Finished iteration")
      }
    }

    //val scommands = if (steptype == "iteration" || steptype == "paramgen") commands else Seq("sleep 3")
    commands map { _.trim } foreach { c =>
      this.synchronized {
        if (!kill && !abort && !c.isEmpty && exitCodes(key) == 0) {
          Logger.info(s"${logPrefix}(${steptype}) Going to run: $c")
          procs(key) = Process(c).run
          Logger.info(s"${logPrefix}(${steptype}) Scheduler waiting for sub-process to finish")
        }
      }

      procs.get(key) map { p =>
        val exitCode = p.exitValue

        /** Save completion information for global access */
        exitCodes(key) = exitCode

        Logger.info(s"${logPrefix}(${steptype}) Sub-process has finished with exit code ${exitCode}")
      }
    }

    finishFlags(key) = true

    /** Display completion information */
    if (kill) Logger.info(s"${logPrefix}(${steptype}) Sub-process was killed upon request")
  }

  override def execute(context: JobExecutionContext): Unit = {
    val jobDataMap = context.getMergedJobDataMap
    val tuneid = jobDataMap.getInt("tuneid")

    val config = Scheduler.config
    val apps = Scheduler.apps
    val engines = Scheduler.engines
    val algos = Scheduler.algos
    val algoInfos = Scheduler.algoInfos
    val offlineEvals = Scheduler.offlineEvals
    val offlineEvalSplitters = Scheduler.offlineEvalSplitters
    val offlineEvalSplitterInfos = Scheduler.offlineEvalSplitterInfos
    val offlineEvalMetrics = Scheduler.offlineEvalMetrics
    val offlineEvalMetricInfos = Scheduler.offlineEvalMetricInfos
    val offlineTunes = Scheduler.offlineTunes
    val paramGens = Scheduler.paramGens
    val paramGenInfos = Scheduler.paramGenInfos

    val logPrefix = s"OfflineTune ID $tuneid: "

    val testMetricParams = collection.immutable.Map("splitset" -> "test")
    val validationMetricParams = collection.immutable.Map(
      "splitset" -> "validation",
      "appdataTestDbType" -> config.appdataValidationDbType,
      "appdataTestDbName" -> config.appdataValidationDbName,
      "appdataTestDbHost" -> config.appdataValidationDbHost,
      "appdataTestDbPort" -> config.appdataValidationDbPort)

    offlineTunes.get(tuneid) map { offlineTune =>
      engines.get(offlineTune.engineid) map { engine =>
        val modelData = config.getModeldataTraining(engine.infoid)
        apps.get(engine.appid) map { app =>
          val totalLoops = offlineTune.loops
          val offlineEvalsToRun = offlineEvals.getByTuneid(offlineTune.id).toSeq

          Logger.info(s"${logPrefix}Starting offline tuning with ${offlineEvalsToRun.size} data set(s) and ${totalLoops} iteration(s) of parameter generation")

          /** Mark the start time */
          val offlineTuneWithStartTime = offlineTune.copy(starttime = Some(DateTime.now))
          offlineTunes.update(offlineTuneWithStartTime)

          /** Data splitting (done only once for each evaluation), and baseline algo evaluation */
          offlineEvalsToRun foreach { offlineEval =>
            val splittersToRun = offlineEvalSplitters.getByEvalid(offlineEval.id).toSeq
            val algosToRun = algos.getByOfflineEvalid(offlineEval.id).toSeq.filter(_.loop.map(_ == 0).getOrElse(false))
            val metricsToRun = offlineEvalMetrics.getByEvalid(offlineEval.id).toSeq
            val algoids = algosToRun map { _.id }
            val metricids = metricsToRun map { _.id }

            /** Delete old model data, if any (usually recovering from an incomplete run) */
            val algosToClean = algos.getByOfflineEvalid(offlineEval.id).toSeq.filter(_.loop.map(_ != 0).getOrElse(false))
            algosToClean foreach { algo =>
              Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Deleting any old model data")
              modelData.delete(algo.id, false)
              algos.delete(algo.id)
            }
            Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Deleting any old app data")
            Scheduler.appdataTrainingUsers.deleteByAppid(offlineEval.id)
            Scheduler.appdataTrainingItems.deleteByAppid(offlineEval.id)
            Scheduler.appdataTrainingU2IActions.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestUsers.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestItems.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestU2IActions.deleteByAppid(offlineEval.id)
            Scheduler.appdataValidationUsers.deleteByAppid(offlineEval.id)
            Scheduler.appdataValidationItems.deleteByAppid(offlineEval.id)
            Scheduler.appdataValidationU2IActions.deleteByAppid(offlineEval.id)

            val currentIteration = 0
            val iterationParam = collection.immutable.Map("iteration" -> currentIteration)
            val splitIterationParam = collection.immutable.Map("iteration" -> 1)

            /** Spiltters setup (support 1 splitter for now) */
            if (splittersToRun.length > 0) {
              val splitter = splittersToRun(0)
              val splitkey = s"split-${offlineEval.id}-${currentIteration}"
              exitCodes(splitkey) = 0
              finishFlags(splitkey) = false

              offlineEvalSplitterInfos.get(splitter.infoid) map { splitterInfo =>
                splitterInfo.commands map { commands =>
                  val splitterCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, None, Some(offlineEval), None, Some(splitterInfo.params.mapValues(_.defaultvalue) ++ splitter.settings ++ splitIterationParam)).toString }
                  step(tuneid, offlineEval.id, currentIteration, "split", splitterCommands)
                } getOrElse {
                  Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Not doing data split because splitter information for ${splitter.infoid} contains no command")
                  step(tuneid, offlineEval.id, currentIteration, "split", Seq())
                }
              } getOrElse {
                Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Not doing data split because splitter information for ${splitter.infoid} is missing")
                step(tuneid, offlineEval.id, currentIteration, "split", Seq())
              }
            }

            algosToRun foreach { algo =>
              val trainingkey = s"training-${offlineEval.id}-${currentIteration}-${algo.id}"

              exitCodes(trainingkey) = 0
              finishFlags(trainingkey) = false

              algoInfos.get(algo.infoid) map { algoInfo =>
                algoInfo.offlineevalcommands map { commands =>
                  val trainingCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, Some(algo), Some(offlineEval), None, Some(iterationParam)).toString }
                  step(tuneid, offlineEval.id, currentIteration, "training", trainingCommands, Some(algo.id))
                } getOrElse {
                  Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Not doing training because algo information for ${algo.infoid} contains no command for offline evaluation")
                  step(tuneid, offlineEval.id, currentIteration, "training", Seq(), Some(algo.id))
                }
              } getOrElse {
                Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Not doing training because algo information for ${algo.infoid} is missing")
                step(tuneid, offlineEval.id, currentIteration, "training", Seq(), Some(algo.id))
              }

              /** Run metrics */
              metricsToRun foreach { metric =>
                val metrickey = s"metric-${offlineEval.id}-${currentIteration}-${algo.id}-${metric.id}"

                exitCodes(metrickey) = 0
                finishFlags(metrickey) = false

                offlineEvalMetricInfos.get(metric.infoid) map { metricInfo =>
                  metricInfo.commands map { commands =>
                    val metricCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, Some(algo), Some(offlineEval), Some(metric), Some(testMetricParams ++ iterationParam)).toString }
                    step(tuneid, offlineEval.id, currentIteration, "metric", metricCommands, Some(algo.id), Some(metric.id))
                  } getOrElse {
                    Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Metric ID ${metric.id}: Not doing training because algo information for ${algo.infoid} contains no command for offline evaluation")
                    step(tuneid, offlineEval.id, currentIteration, "metric", Seq(), Some(algo.id), Some(metric.id))
                  }
                } getOrElse {
                  Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Metric ID ${metric.id}: Not doing training because algo information for ${algo.infoid} is missing")
                  step(tuneid, offlineEval.id, currentIteration, "metric", Seq(), Some(algo.id), Some(metric.id))
                }
              }
            }

            /** Block on baseline training and metric */
            val iterationkey = s"iteration-${offlineEval.id}-${currentIteration}"
            exitCodes(iterationkey) = 0
            finishFlags(iterationkey) = false

            step(tuneid, offlineEval.id, currentIteration, "iteration", Seq(), None, None, Some(algoids), Some(metricids))
          }

          /** Start iterative tuning */

          /** Parameter generator setup (support 1 param gen for now) */
          /** Parameter generator is run once for all offline evaluations */
          val tuneSubject = algos.getTuneSubjectByOfflineTuneid(tuneid).get
          val paramGensToRun = paramGens.getByTuneid(tuneid).toSeq
          val paramGen = paramGensToRun(0)
          val paramGenInfo = paramGenInfos.get(paramGen.infoid)
          val paramGenParams = collection.immutable.Map("algoid" -> tuneSubject.id, "paramsets" -> 1, "evalids" -> offlineEvalsToRun.map(_.id.toString).reduce((a, b) => s"${a},${b}"))

          for (currentLoop <- 1 to totalLoops) {
            val iterationParam = collection.immutable.Map("iteration" -> currentLoop)
            val loopParam = collection.immutable.Map("loop" -> currentLoop)

            val paramGenKey = s"paramgen-0-${currentLoop}"
            if (paramGensToRun.length > 0) {
              val paramGen = paramGensToRun(0)
              exitCodes(paramGenKey) = 0
              finishFlags(paramGenKey) = false

              paramGenInfo map { paramGenInfo =>
                paramGenInfo.commands map { commands =>
                  val paramGenCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, None, None, None, Some(paramGenInfo.paramdefaults ++ paramGen.params ++ loopParam ++ paramGenParams)).toString }
                  step(tuneid, 0, currentLoop, "paramgen", paramGenCommands)
                } getOrElse {
                  Logger.warn(s"${logPrefix}: Not generating parameters because generator information for ${paramGen.infoid} contains no command")
                  step(tuneid, 0, currentLoop, "paramgen", Seq())
                }
              } getOrElse {
                Logger.warn(s"${logPrefix}: Not generating parameters because generator information for ${paramGen.infoid} is missing")
                step(tuneid, 0, currentLoop, "paramgen", Seq())
              }
            }

            /** Block on param gen */
            while (!finishFlags(paramGenKey)) {
              Thread.sleep(1000)
            }

            /** Evaluate generated algos */
            offlineEvalsToRun foreach { offlineEval =>
              /** Only 1 param set for now */
              val algosToRun = algos.getByOfflineEvalid(offlineEval.id, Some(currentLoop), Some(1)).toSeq
              val metricsToRun = offlineEvalMetrics.getByEvalid(offlineEval.id).toSeq
              val algoids = algosToRun map { _.id }
              val metricids = metricsToRun map { _.id }

              algosToRun foreach { algo =>
                val trainingkey = s"training-${offlineEval.id}-${currentLoop}-${algo.id}"

                exitCodes(trainingkey) = 0
                finishFlags(trainingkey) = false

                algoInfos.get(algo.infoid) map { algoInfo =>
                  algoInfo.offlineevalcommands map { commands =>
                    val trainingCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, Some(algo), Some(offlineEval), None, Some(loopParam)).toString }
                    step(tuneid, offlineEval.id, currentLoop, "training", trainingCommands, Some(algo.id))
                  } getOrElse {
                    Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Not doing training because algo information for ${algo.infoid} contains no command for offline evaluation")
                    step(tuneid, offlineEval.id, currentLoop, "training", Seq(), Some(algo.id))
                  }
                } getOrElse {
                  Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Not doing training because algo information for ${algo.infoid} is missing")
                  step(tuneid, offlineEval.id, currentLoop, "training", Seq(), Some(algo.id))
                }

                /** Run metrics */
                metricsToRun foreach { metric =>
                  val metrickey = s"metric-${offlineEval.id}-${currentLoop}-${algo.id}-${metric.id}"

                  exitCodes(metrickey) = 0
                  finishFlags(metrickey) = false

                  offlineEvalMetricInfos.get(metric.infoid) map { metricInfo =>
                    metricInfo.commands map { commands =>
                      val validationMetricCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, Some(algo), Some(offlineEval), Some(metric), Some(validationMetricParams ++ iterationParam)).toString }
                      val testMetricCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, Some(algo), Some(offlineEval), Some(metric), Some(testMetricParams ++ iterationParam)).toString }
                      step(tuneid, offlineEval.id, currentLoop, "metric", validationMetricCommands ++ testMetricCommands, Some(algo.id), Some(metric.id))
                    } getOrElse {
                      Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Metric ID ${metric.id}: Not doing training because algo information for ${algo.infoid} contains no command for offline evaluation")
                      step(tuneid, offlineEval.id, currentLoop, "metric", Seq(), Some(algo.id), Some(metric.id))
                    }
                  } getOrElse {
                    Logger.warn(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Metric ID ${metric.id}: Not doing training because algo information for ${algo.infoid} is missing")
                    step(tuneid, offlineEval.id, currentLoop, "metric", Seq(), Some(algo.id), Some(metric.id))
                  }
                }
              }

              /** Block on training and metric */
              val iterationkey = s"iteration-${offlineEval.id}-${currentLoop}"
              exitCodes(iterationkey) = 0
              finishFlags(iterationkey) = false

              step(tuneid, offlineEval.id, currentLoop, "iteration", Seq(), None, None, Some(algoids), Some(metricids))
            }
          }

          /** Block on last iterations of all offline evaluations */
          val offlineEvalKeys = offlineEvalsToRun map { o => s"iteration-${o.id}-${totalLoops}" }

          while (!finishFlags.filterKeys(offlineEvalKeys.contains(_)).values.reduce((a, b) => a && b)) {
            Thread.sleep(1000)
          }

          /** Check for errors from metric */
          Logger.info(s"${logPrefix}Exit code summary:")

          Logger.info(s"${logPrefix}Iteration 0:")
          offlineEvalsToRun foreach { offlineEval =>
            val currentIteration = 0
            val algosToRun = algos.getByOfflineEvalid(offlineEval.id).toSeq.filter(_.loop.map(_ == currentIteration).getOrElse(false))
            val metricsToRun = offlineEvalMetrics.getByEvalid(offlineEval.id).toSeq
            val algoids = algosToRun map { _.id }
            val metricids = metricsToRun map { _.id }
            Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}:   Split: " + exitCodes(s"split-${offlineEval.id}-${currentIteration}"))
            algoids foreach { algoid =>
              Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}:   Algo ID ${algoid}: " + exitCodes(s"training-${offlineEval.id}-${currentIteration}-${algoid}"))
              metricids foreach { metricid =>
                Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}:     Metric ID ${metricid}: " + exitCodes(s"metric-${offlineEval.id}-${currentIteration}-${algoid}-${metricid}"))
              }
            }
          }
          for (currentLoop <- 1 to totalLoops) {
            Logger.info(s"${logPrefix}Iteration ${currentLoop}:")
            offlineEvalsToRun foreach { offlineEval =>
              val algosToRun = algos.getByOfflineEvalid(offlineEval.id, Some(currentLoop), Some(1)).toSeq
              val metricsToRun = offlineEvalMetrics.getByEvalid(offlineEval.id).toSeq
              val algoids = algosToRun map { _.id }
              val metricids = metricsToRun map { _.id }
              algoids foreach { algoid =>
                Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}:   Algo ID ${algoid}: " + exitCodes(s"training-${offlineEval.id}-${currentLoop}-${algoid}"))
                metricids foreach { metricid =>
                  Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}:     Metric ID ${metricid}: " + exitCodes(s"metric-${offlineEval.id}-${currentLoop}-${algoid}-${metricid}"))
                }
              }
            }
          }

          if (exitCodes.values.sum != 0)
            Logger.warn(s"${logPrefix}Offline tuning completed with error(s)")
          else
            Logger.info(s"${logPrefix}Offline tuning completed")

          /** Mark the end time since this is used to determine whether the run has finished */
          offlineTunes.update(offlineTuneWithStartTime.copy(endtime = Some(DateTime.now)))

          /** Mark subject algo as tuned */
          algos.update(tuneSubject.copy(status = "tuned"))

          /** Clean up */
          offlineEvalsToRun foreach { offlineEval =>
            val algosToClean = algos.getByOfflineEvalid(offlineEval.id).toSeq.filter(_.loop.map(_ != 0).getOrElse(false))
            algosToClean foreach { algo =>
              Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Algo ID ${algo.id}: Deleting used model data")
              modelData.delete(algo.id, false)
            }
            Logger.info(s"${logPrefix}OfflineEval ID ${offlineEval.id}: Deleting used app data")
            Scheduler.appdataTrainingUsers.deleteByAppid(offlineEval.id)
            Scheduler.appdataTrainingItems.deleteByAppid(offlineEval.id)
            Scheduler.appdataTrainingU2IActions.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestUsers.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestItems.deleteByAppid(offlineEval.id)
            Scheduler.appdataTestU2IActions.deleteByAppid(offlineEval.id)
            Scheduler.appdataValidationUsers.deleteByAppid(offlineEval.id)
            Scheduler.appdataValidationItems.deleteByAppid(offlineEval.id)
            Scheduler.appdataValidationU2IActions.deleteByAppid(offlineEval.id)
          }
        } getOrElse {
          Logger.warn(s"${logPrefix}Not starting offline tuning because the app that owns this offline tuning cannot be found from the database")
        }
      } getOrElse {
        Logger.warn(s"${logPrefix}Not starting offline tuning because the engine that owns this offline tuning cannot be found from the database")
      }
    } getOrElse {
      Logger.warn(s"${logPrefix}Not starting offline tuning because the offline tuning cannot be found from the database")
    }
  }

  override def interrupt() = {
    this.synchronized {
      kill = true
      procs.values map { _.destroy }
    }
  }
}
