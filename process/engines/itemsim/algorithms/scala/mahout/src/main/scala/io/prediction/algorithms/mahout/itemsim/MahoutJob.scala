package io.prediction.algorithms.mahout.itemsim

import grizzled.slf4j.Logger
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

import scala.io.Source
import scala.collection.JavaConversions._
import scala.sys.process._
import scala.collection.mutable.PriorityQueue

import com.github.nscala_time.time.Imports._
import org.apache.mahout.cf.taste.similarity.ItemSimilarity
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel

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

    logger.info("done")

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
  def getArgOpt(args: Map[String, String], key: String, default: String): String = {
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

  /** create and return Mahout's ItemSimilarity object. */
  def buildItemSimilarity(dataModel: DataModel, args: Map[String, String]): ItemSimilarity

  /**
   * Run algo job.
   * In default implementation, the prepare() function does nothing
   * The run() function read and process this local file (defined by --input arg) file and generate the prediction
   * output file (defined by --output arg) for each user.
   * Then finish() does nothing
   */
  def run(args: Map[String, String]): Map[String, String] = {

    val input = args("input")
    val output = args("output")
    val itemsFile = args("itemsFile") // contains valid item index can be recommended
    val numSimilarItems: Int = getArgOpt(args, "numSimilarItems", "10").toInt
    val recommendationTime: Long = getArg(args, "recommendationTime").toLong
    val freshness = getArgOpt(args, "freshness", "0").toInt
    val freshnessTimeUnit: Long = getArgOpt(args, "freshnessTimeUnit")
      .map(_.toLong).getOrElse(1.hours.millis)

    // valid item index file (iindex)
    // iindex
    val validItemsMap: Map[Long, Long] = Source.fromFile(itemsFile).getLines()
      .map { line =>
        val (iindex, starttime) = try {
          val fields = line.split("\t")
          (fields(0).toLong, fields(1).toLong)
        } catch {
          case e: Exception => {
            throw new RuntimeException(s"Cannot get item info in line: ${line}. ${e}")
          }
        }
        (iindex, starttime)
      }.toMap

    val validItemsSet = validItemsMap.keys.toSet

    val dataModel: DataModel = new FileDataModel(new File(input))
    val similarity: ItemSimilarity = buildItemSimilarity(dataModel, args)

    val outputFile = new File(output)
    // create dir if it doesn't exist yet.
    outputFile.getParentFile().mkdirs()

    // generate prediction output file
    val outputWriter = new BufferedWriter(new FileWriter(outputFile))

    val itemIds = dataModel.getItemIDs.toSeq.map(_.toLong)
    val candidateItemsIds = itemIds.filter(validItemsSet(_))

    val allTopScores = itemIds.par.map { iid =>
      val simScores = candidateItemsIds
        .map { simiid =>
          val originalScore = similarity.itemSimilarity(iid, simiid)
          val score = if (freshness > 0) {
            validItemsMap.get(simiid) map { starttime =>
              val timeDiff = (recommendationTime - starttime) / 1000.0 /
                freshnessTimeUnit
              if (timeDiff > 0)
                originalScore * scala.math.exp(-timeDiff / (11 - freshness))
              else
                originalScore
            } getOrElse originalScore
          } else originalScore
          (simiid, score)
        }
        // filter out invalid score or the same iid itself
        .filter { x: (Long, Double) => (!x._2.isNaN()) && (x._1 != iid) }

      (iid, getTopN(simScores, numSimilarItems)(ScoreOrdering.reverse))
    }

    allTopScores.seq.foreach {
      case (iid, simScores) =>
        if (!simScores.isEmpty) {
          val scoresString = simScores.map(x => s"${x._1}:${x._2}")
            .mkString(",")
          outputWriter.write(s"${iid}\t[${scoresString}]\n")
        }
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

  object ScoreOrdering extends Ordering[(Long, Double)] {
    override def compare(a: (Long, Double), b: (Long, Double)) =
      a._2 compare b._2
  }

  def getTopN[T](s: Seq[T], n: Int)(ord: Ordering[T]): Seq[T] = {
    val q = PriorityQueue()(ord)

    for (x <- s) {
      if (q.size < n)
        q.enqueue(x)
      else {
        // q is full
        if (ord.compare(x, q.head) < 0) {
          q.dequeue()
          q.enqueue(x)
        }
      }
    }

    q.dequeueAll.toSeq.reverse
  }
}
