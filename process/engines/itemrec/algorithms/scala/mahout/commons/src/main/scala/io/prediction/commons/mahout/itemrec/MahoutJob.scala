package io.prediction.commons.mahout.itemrec

import java.io.File
import java.io.FileWriter

import scala.sys.process._

import io.prediction.commons.filepath.{DataFile, AlgoFile}
import io.prediction.commons.Config

/** main function to run non-distributed Mahout Job */
object MahoutJob {

  def main(args: Array[String]) {
    if (args.size < 1) {
      println("Please specify Mahout job class name")
      println("Example. <job class name> --param1 1 --param2 2")
      System.exit(1)
    }
    
    val jobName = args(0)

    println("Running Job %s...".format(jobName))

    //println(args.mkString(" "))
    val (argMap, lastkey) = args.drop(1).foldLeft((Map[String, String](), "")) { (res, data) => 
      val (argMap, lastkey) = res
      val key: Option[String] = if (data.startsWith("--")) Some(data.stripPrefix("--")) else None

      key map { k =>
        (argMap ++ Map(k -> ""), k)
      } getOrElse {
        val orgData = argMap(lastkey)
        val newData = orgData match {
          case "" => data
          case _ => orgData + " " + data
        } 
        (argMap ++ Map(lastkey -> newData), lastkey)
      }
    }
    //println(argMap)

    val job = Class.forName(jobName).
      getConstructor().
      newInstance().
      asInstanceOf[MahoutJob]
  
    val runArgs = job.prepare(argMap)
    
    val finishArgs = job.run(runArgs)

    val cleanupArgs = job.finish(finishArgs)

    job.cleanup(cleanupArgs)

  }

}

/** Wrapper job class for Mahout algo */
abstract class MahoutJob {

  val commonsConfig = new Config
  /** Try search path if hadoop home is not set. */
  val hadoopCommand = commonsConfig.settingsHadoopHome map { h => h+"/bin/hadoop" } getOrElse { "hadoop" }

  /** Get required arg */
  def getArg(args: Map[String, String], key: String): String = {
    if (!args.contains(key)) sys.error("Please specify value for parameter --" + key)

    args(key)
  }

  /** Get optional arg */
  def getArgOpt(args: Map[String, String], key: String, default: String): String = {
    if (args.contains(key)) args(key) else default
  }

  def getArgOpt(args: Map[String, String], key: String): Option[String] = {
    if (args.contains(key)) Some(args(key)) else None
  }

  /** Prepare stage for algo */
  def prepare(args: Map[String, String]): Map[String, String] = {
    
    val hdfsRoot = getArg(args, "hdfsRoot") // required
    val localTempRoot = getArg(args, "localTempRoot") // required
    val appid = getArg(args, "appid").toInt // required
    val engineid = getArg(args, "engineid").toInt // required
    val algoid = getArg(args, "algoid").toInt // required
    val evalid: Option[Int] = getArgOpt(args, "evalid") map { _.toInt }

    // input file
    val hdfsRatingsPath = DataFile(hdfsRoot, appid, engineid, algoid, evalid, "ratings.csv")

    val localRatingsPath = localTempRoot + "algo-" + algoid + "-ratings.csv"
    val localRatingsFile = new File(localRatingsPath)
    localRatingsFile.getParentFile().mkdirs() // create parent dir
    if (localRatingsFile.exists()) localRatingsFile.delete() // delete existing file first

    val copyFromHdfsRatingsCmd = s"$hadoopCommand fs -getmerge $hdfsRatingsPath $localRatingsPath"
    //logger.info("Executing '%s'...".format(copyFromHdfsRatingsCmd))
    println("Executing '%s'...".format(copyFromHdfsRatingsCmd))
    if ((copyFromHdfsRatingsCmd.!) != 0)
      throw new RuntimeException("Failed to execute '%s'".format(copyFromHdfsRatingsCmd))

    // output file
    val localPredictedPath = localTempRoot + "algo-"+ algoid + "-predicted.tsv"
    val localPredictedFile = new File(localPredictedPath)

    localPredictedFile.getParentFile().mkdirs() // create parent dir
    if (localPredictedFile.exists()) localPredictedFile.delete() // delete existing file first

    val hdfsPredictedPath = AlgoFile(hdfsRoot, appid, engineid, algoid, evalid, "predicted.tsv")

    args ++ Map("input" -> localRatingsPath, "output" -> localPredictedPath, "hdfsOutput" -> hdfsPredictedPath)
  }

  /** Run algo job.
    In default implementation, the prepare() function copies the ratings.csv from HDFS to local temporary directory.
    The run() function should read and process this local file (defined by --input arg) file and generate the prediction 
    output file (defined by --output arg) for each user.
    Then finish() function copies the local prediction output file to HDFS predicted.tsv
  */
  def run(args: Map[String, String]): Map[String, String]

  /** finish stage for algo */
  def finish(args: Map[String, String]): Map[String, String] = {

    val localPredictedPath = args("output") // required
    val hdfsPredictedPath = args("hdfsOutput")

    // delete the hdfs file if it exists, otherwise copyFromLocal will fail.
    val deleteHdfsPredictedCmd = s"$hadoopCommand fs -rmr $hdfsPredictedPath"
    val copyToHdfsPredictedCmd = s"$hadoopCommand fs -copyFromLocal $localPredictedPath $hdfsPredictedPath"

    //logger.info("Executing '%s'...".format(deleteHdfsPredictedCmd))
    println("Executing '%s'...".format(deleteHdfsPredictedCmd))
    deleteHdfsPredictedCmd.!
    
    //logger.info("Executing '%s'...".format(copyToHdfsPredictedCmd))
    println("Executing '%s'...".format(copyToHdfsPredictedCmd))
    if ((copyToHdfsPredictedCmd.!) != 0)
      throw new RuntimeException("Failed to execute '%s'".format(copyToHdfsPredictedCmd))

    args
  }

  /** Cleanup stage for algo */
  def cleanup(args: Map[String, String]) = {
    val localRatingsPath = args("input") // required
    val localPredictedPath = args("output") // required
    
    val localRatingsFile = new File(localRatingsPath)
    val localPredictedFile = new File(localPredictedPath)

    //logger.info("Deleting temporary file " + localRatingsFile.getPath)
    println("Deleting temporary file %s...".format(localRatingsFile.getPath))
    localRatingsFile.delete()
    //logger.info("Deleting temporary file " + localPredictedFile.getPath)
    println("Deleting temporary file %s...".format(localPredictedFile.getPath))
    localPredictedFile.delete()

    args
  }

}
