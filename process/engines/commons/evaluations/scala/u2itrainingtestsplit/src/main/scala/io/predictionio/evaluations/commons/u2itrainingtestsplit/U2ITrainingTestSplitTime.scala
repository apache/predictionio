package io.prediction.evaluations.commons.trainingtestsplit

import com.twitter.scalding.Args

import io.prediction.commons.filepath.{U2ITrainingTestSplitFile}

import java.io.File
import scala.io.Source
import scala.sys.process._

/**
 * Wrapper for Scalding U2ITrainingTestSplitTime job
 *
 * Args:
 * --hadoop <string> hadoop command
 * --pdioEvalJar <string> the name of the Scalding U2ITrainingTestSplit job jar
 * --sequenceNum. <int>. the sequence number (starts from 1 for the 1st iteration and then increment for later iterations)
 * 
 * --dbType: <string> appdata DB type
 * --dbName: <string>
 * --dbHost: <string>. optional. (eg. "127.0.0.1")
 * --dbPort: <int>. optional. (eg. 27017)
 *
 * --training_dbType: <string> training_appadta DB type
 * --training_dbName: <string>
 * --training_dbHost: <string>. optional
 * --training_dbPort: <int>. optional
 * 
 * --validation_dbType: <string> validation_appdata DB type
 * --validation_dbName: <string>
 * --validation_dbHost: <string>. optional
 * --validation_dbPort: <int>. optional
 *
 * --test_dbType: <string> test_appdata DB type
 * --test_dbName: <string>
 * --test_dbHost: <string>. optional
 * --test_dbPort: <int>. optional
 *
 * --hdfsRoot: <string>. Root directory of the HDFS
 * 
 * --appid: <int>
 * --engineid: <int>
 * --evalid: <int>
 *
 * --itypes: <string separated by white space>. eg "--itypes type1 type2". If no --itypes specified, then ALL itypes will be used.
 *
 * --trainingPercent: <double> (0.01 to 1). training set percentage
 * --validationPercent: <dboule> (0.01 to 1). validation set percentage
 * --testPercent: <double> (0.01 to 1). test set percentage
 * --timeorder: <boolean>. Require total percentage < 1
 *
 */
object U2ITrainingTestSplitTime {

  def main(mainArgs: Array[String]) {

    /** parse args*/
    val args = Args(mainArgs)

    val hadoop = args("hadoop")
    val pdioEvalJar = args("pdioEvalJar")
    
    val hdfsRoot = args("hdfsRoot")
    val localTempRoot = args("localTempRoot")

    val appid = args("appid").toInt
    val engineid = args("engineid").toInt
    val evalid = args("evalid").toInt

    val sequenceNum = args("sequenceNum").toInt
    require((sequenceNum >= 1), "sequenceNum must be >= 1.")

    val argsString = args.toString

    val resplit: Boolean = (sequenceNum > 1)

    /** command */
    if (!resplit) {
      // prep
      val splitPrepCmd = hadoop + " jar " + pdioEvalJar + " io.prediction.evaluations.scalding.commons.u2itrainingtestsplit.U2ITrainingTestSplitTimePrep " + argsString
      executeCommandAndCheck(splitPrepCmd)

    }

    // copy the count to local tmp
    val hdfsCountPath = U2ITrainingTestSplitFile(hdfsRoot, appid, engineid, evalid, "u2iCount.tsv")
    val localCountPath = localTempRoot + "eval-" + evalid + "-u2iCount.tsv"
    
    val localCountFile = new File(localCountPath)

    // create parent dir
    localCountFile.getParentFile().mkdirs()

    // delete existing file first
    if (localCountFile.exists()) localCountFile.delete() 

    // get the count from hdfs
    val getHdfsCountCmd = hadoop + " fs -getmerge " + hdfsCountPath + " " + localCountPath
    executeCommandAndCheck(getHdfsCountCmd)

    // read the local file and get the count
    val lines = Source.fromFile(localCountPath).getLines
    if (lines.isEmpty)
      throw new RuntimeException("Count file %s is empty".format(localCountPath))
    
    val count = lines.next

    // split
    val splitCmd = hadoop +" jar " + pdioEvalJar + " io.prediction.evaluations.scalding.commons.u2itrainingtestsplit.U2ITrainingTestSplitTime " + argsString + " --totalCount " + count
    executeCommandAndCheck(splitCmd)

    // delete local tmp file
    println("Deleting temporary file %s...".format(localCountPath))
    localCountFile.delete()

  }

  def executeCommandAndCheck(cmd: String) = {
    println("Executing %s...".format(cmd))
    if ((cmd.!) != 0)
      throw new RuntimeException("Failed to execute '%s'".format(cmd)) 
  }
  
}



