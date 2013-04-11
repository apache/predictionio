package io.prediction.evaluations.itemrec.trainingtestsplit

import com.twitter.scalding.Args

import io.prediction.commons.filepath.{TrainingTestSplitFile}

import java.io.File
import scala.io.Source
import scala.sys.process._

/**
 * wrapper for Scalding TrainingTestSplit job
 */
object TrainingTestSplit {

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

    val argsString = args.toString

    /** command */
    // prep
    val splitPrepCmd = hadoop + " jar " + pdioEvalJar + " io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplitTimePrep " + argsString

    executeCommandAndCheck(splitPrepCmd)

    // copy the count to local tmp
    val hdfsCountPath = TrainingTestSplitFile(hdfsRoot, appid, engineid, evalid, "u2iCount.tsv")
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
    val splitCmd = hadoop +" jar " + pdioEvalJar + " io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplitTime " + argsString + " --totalCount " + count
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



