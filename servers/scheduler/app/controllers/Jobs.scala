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

import scala.collection.mutable.{HashMap, Map, SynchronizedMap}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.sys.process._

object Jobs {
  val algoJobGroup = "predictionio-algo"
  val offlineEvalJobGroup = "predictionio-offlineeval"

  def algoJob(config: Config, app: App, engine: Engine, algo: Algo, batchcommands: Seq[String]) = {
    /** Build command from template. */
    val command = new StringTemplate(batchcommands.mkString(" && "))
    setSharedAttributes(command, config, app, engine, Some(algo), None, None)

    /** Add a job, then build a trigger for it.
      * This is necessary for updating any existing job,
      * and make sure the trigger will fire.
      */
    val job = newJob(classOf[AlgoJob]) withIdentity(algo.id.toString, algoJobGroup) storeDurably(true) build()
    job.getJobDataMap().put("template", command.toString)
    job.getJobDataMap().put("algoid", algo.id)
    job.getJobDataMap().put("engineinfoid", engine.infoid)

    job
  }

  /** Offline Evaluation Flow
    *
    * 1. Iterate the following for a specified number of times
    *    1. Perform data splitting
    *    2. For each algo to be evaluated
    *       1. Run algo on training set
    *       2. Run all metrics on model data from the above against test set
    * 2. Mark offline evaluation as finished
    */
  def offlineEvalJob(config: Config, app: App, engine: Engine, offlineEval: OfflineEval) = {
    /** Add a job, then build a trigger for it.
      * This is necessary for updating any existing job,
      * and make sure the trigger will fire.
      */
    val job = newJob(classOf[OfflineEvalJob]) withIdentity(offlineEval.id.toString, offlineEvalJobGroup) storeDurably(true) build()
    job.getJobDataMap().put("evalid", offlineEval.id)
    job
  }

  def setSharedAttributes(command: StringTemplate, config: Config, app: App, engine: Engine, algo: Option[Algo], offlineEval: Option[OfflineEval], metric: Option[OfflineEvalMetric], params: Option[collection.immutable.Map[String, Any]] = None) = {
    /** Custom attributes */
    params map { command.setAttributes(_) }

    /** OfflineEvalMetric-specific attributes */
    metric map { met =>
      command.setAttributes(command.attributes ++ met.params)
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
      val defaultParams = Scheduler.algoInfos.get(alg.infoid) map { _.paramdefaults } getOrElse Map[String, String]()
      command.setAttributes(command.attributes ++ defaultParams ++ alg.params)
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

    /** TODO: These JAR naming and locations must be generalized */
    command.setAttribute("pdioEvalJar", config.getJar("io.prediction.evaluations.scalding.itemrec").getOrElse(""))
    command.setAttribute("mahoutCoreJobJar", config.getJar("io.prediction.algorithms.mahout-core-job").getOrElse(""))
    command.setAttribute("itemrecScalaMahoutJar", config.getJar("io.prediction.algorithms.mahout.itemrec").getOrElse(""))
    command.setAttribute("topkJar", config.getJar("io.prediction.evaluations.itemrec.topkitems").getOrElse(""))
    command.setAttribute("trainingTestSplitTimeJar", config.getJar("io.prediction.evaluations.itemrec.trainingtestsplit").getOrElse(""))

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
    command.setAttribute("appdataValidationDbType", config.appdataValidationDbType)
    command.setAttribute("appdataValidationDbName", config.appdataValidationDbName)
    command.setAttribute("appdataValidationDbHost", config.appdataValidationDbHost)
    command.setAttribute("appdataValidationDbPort", config.appdataValidationDbPort)
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
    val engineinfoid = jobDataMap.getString("engineinfoid")
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
        engineinfoid match {
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
          val iterationkey = s"iteration-${iteration-1}"
          while (!finishFlags(iterationkey)) {
            Thread.sleep(1000)
          }
        }
      }
      case "training" => {
        val splitkey = s"split-${iteration}"
        while (!finishFlags(splitkey)) {
          Thread.sleep(1000)
        }
        if (exitCodes(splitkey) != 0) {
          abort = true
          Logger.info(s"${logPrefix}(${steptype}) Aborted due to split error")
        }
      }
      case "metric" => {
        val trainingkey = s"training-${iteration}-${algoid.get}"
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
        } yield s"metric-${iteration}-${aid}-${mid}"

        while (!finishFlags.filterKeys(keys.contains(_)).values.reduce((a, b) => a && b)) {
          Thread.sleep(1000)
        }

        Logger.info(s"${logPrefix}(${steptype}) Finished iteration")
      }
    }

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
        apps.get(engine.appid) map {app =>

          val totalIterations = offlineEval.iterations
          val splittersToRun = offlineEvalSplitters.getByEvalid(offlineEval.id).toSeq
          val algosToRun = algos.getByOfflineEvalid(offlineEval.id).toSeq
          val metricsToRun = offlineEvalMetrics.getByEvalid(offlineEval.id).toSeq

          val algoids = algosToRun map { _.id }
          val metricids = metricsToRun map { _.id }

          Logger.info(s"${logPrefix}Starting offline evaluation with ${totalIterations} iteration(s)")

          /** Mark the start time */
          offlineEvals.update(offlineEval.copy(starttime = Some(DateTime.now)))

          /** Delete old model data, if any (usually recovering from an incomplete run) */
          engine.infoid match {
            case "itemrec" => algosToRun foreach { algo =>
              Logger.info(s"${logPrefix}Algo ID ${algo.id}: Deleting any old model data")
              Scheduler.itemRecScores.deleteByAlgoid(algo.id)
            }
          }

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
                  val splitterCommands = commands map { c => Jobs.setSharedAttributes(new StringTemplate(c), config, app, engine, None, Some(offlineEval), None, Some(splitterInfo.paramdefaults ++ iterationParam)).toString }
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

          /** Check for errors from metric */
          println(s"${logPrefix}Exit code summary:")

          for (currentIteration <- 1 to totalIterations) {
            println(s"${logPrefix}Iteration ${currentIteration}:")
            println(s"${logPrefix}  Split: "+exitCodes(s"split-${currentIteration}"))
            algoids foreach { algoid =>
              println(s"${logPrefix}  Algo ID ${algoid}: "+exitCodes(s"training-${currentIteration}-${algoid}"))
              metricids foreach { metricid =>
                println(s"${logPrefix}    Metric ID ${metricid}: "+exitCodes(s"metric-${currentIteration}-${algoid}-${metricid}"))
              }
            }
          }

          if (exitCodes.values.sum != 0)
            Logger.warn(s"${logPrefix}Offline evaluation completed with error(s)")
          else
            Logger.info(s"${logPrefix}Offline evaluation completed")

          /** Mark the end time since this is used to determine whether the run has finished */
          offlineEvals.update(offlineEval.copy(endtime = Some(DateTime.now)))

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
