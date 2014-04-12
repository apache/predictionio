package io.prediction.algorithms.mahout.itemrec

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

import scala.collection.JavaConversions._
import scala.sys.process._

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logger
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.{ IDRescorer, Recommender }

/** main function to run non-distributed Mahout Job */
object MahoutJob {

  val logger = Logger(MahoutJob.getClass)

  def main(args: Array[String]) {
    if (args.size < 1) {
      logger.error("Please specify Mahout job class name")
      logger.error("Example. <job class name> --param1 1 --param2 2")
      System.exit(1)
    }

    val jobName = args(0)

    logger.info("Running Job %s...".format(jobName))

    logger.info(args.mkString(" "))
    val (argMap, lastkey) = args.drop(1).foldLeft((Map[String, String](), "")) {
      (res, data) =>
        val (argMap, lastkey) = res
        val key: Option[String] = if (data.startsWith("--"))
          Some(data.stripPrefix("--")) else None

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
  /** Get required arg */
  def getArg(args: Map[String, String], key: String): String = {
    if (!args.contains(key)) sys.error("Please specify value for parameter --" + key)

    args(key)
  }

  /** Get optional arg */
  def getArgOpt(args: Map[String, String], key: String,
    default: String): String = {
    if (args.contains(key)) args(key) else default
  }

  def getArgOpt(args: Map[String, String], key: String): Option[String] = {
    if (args.contains(key)) Some(args(key)) else None
  }

  /** Prepare stage for algo */
  def prepare(args: Map[String, String]): Map[String, String] = {
    // simply pass the args to next stage
    args
  }

  /** create and return Mahout's Recommender object. */
  def buildRecommender(dataModel: DataModel, seenDataModel: DataModel,
    args: Map[String, String]): Recommender

  /**
   * Run algo job.
   * In default implementation, the prepare() function does nothing
   * The run() function read and process this local file (defined by --input
   * arg) file and generate the prediction output file (defined by --output arg)
   * for each user. Then finish() does nothing
   */
  def run(args: Map[String, String]): Map[String, String] = {

    val input = args("input")
    val output = args("output")

    val numRecommendations: Int = getArgOpt(args, "numRecommendations", "10")
      .toInt
    val recommendationTime: Long = getArg(args, "recommendationTime").toLong
    val freshnessTimeUnit: Long = getArgOpt(args, "freshnessTimeUnit")
      .map(_.toLong).getOrElse(1.hours.millis)
    val inputDir: String = getArgOpt(args, "inputDir", "")
    /** use input ratng file as seen data if it's not defined */
    val seenFileOpt = getArgOpt(args, "seenFile")
    val freshnessOpt = getArgOpt(args, "freshness")

    val dataModel: DataModel = new FileDataModel(new File(input))

    val seenDataModel: DataModel = seenFileOpt.map { seenFileName =>
      val seenFile = new File(seenFileName)
      if (seenFile.exists())
        if (seenFile.length() != 0) // if not empty
          new FileDataModel(seenFile)
        else
          null
      else
        dataModel // fall back to rating dataModel if no seenFile defined
    }.getOrElse(dataModel)

    val recommender: Recommender = buildRecommender(dataModel, seenDataModel,
      args)

    val outputFile = new File(output)
    // create dir if it doesn't exist yet.
    outputFile.getParentFile().mkdirs()

    // handle freshness rescoring
    val freshnessRescorer = freshnessOpt map { f =>
      new FreshnessRescorer(f.toInt, recommendationTime, freshnessTimeUnit,
        MahoutCommons.itemsMap(s"${inputDir}itemsIndex.tsv"))
    }

    // generate prediction output file
    val userRec = dataModel.getUserIDs.toSeq.par
      .map { uid =>
        val rec = freshnessRescorer map { r =>
          recommender.recommend(uid, numRecommendations, r)
        } getOrElse {
          recommender.recommend(uid, numRecommendations)
        }
        if (rec.size != 0) {
          val prediction = uid + "\t" + "[" + (rec map { x =>
            x.getItemID + ":" + x.getValue
          }).mkString(",") + "]"
          Some(prediction)
        } else {
          None
        }
      }

    val outputWriter = new BufferedWriter(new FileWriter(outputFile))
    userRec.seq.foreach { line =>
      line.map(v => outputWriter.write(v + "\n"))
    }
    outputWriter.close()

    args
  }

  /** finish stage for algo */
  def finish(args: Map[String, String]): Map[String, String] = {
    // simply pass the args to next stage
    args
  }

  /** Cleanup stage for algo */
  def cleanup(args: Map[String, String]) = {
    // simpley pass the args to next stage
    args
  }

}

class FreshnessRescorer(freshness: Int, recommendationTime: Long,
    freshnessTimeUnit: Long,
    itemsMap: Map[Long, MahoutCommons.ItemData]) extends IDRescorer {
  def isFiltered(id: Long): Boolean = false

  def rescore(id: Long, originalScore: Double): Double = {
    if (freshness > 0) {
      itemsMap.get(id) map { data =>
        val timeDiff = (recommendationTime - data.starttime) / 1000 /
          freshnessTimeUnit
        if (timeDiff > 0)
          originalScore * scala.math.exp(-timeDiff / (11 - freshness))
        else
          originalScore
      } getOrElse originalScore
    } else originalScore
  }
}
