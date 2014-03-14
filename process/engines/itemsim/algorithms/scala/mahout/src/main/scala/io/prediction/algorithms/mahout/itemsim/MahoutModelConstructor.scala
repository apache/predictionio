package io.prediction.algorithms.mahout.itemsim

import grizzled.slf4j.Logger
import com.twitter.scalding.Args
import scala.io.Source

import io.prediction.commons.Config
import io.prediction.commons.modeldata.{ ItemSimScore }

/*
 * Description:
 * input file:
 * - itemsIndex.tsv (iindex iid itypes): all items
 * - similarities.tsv (iindex [iindex1:score,iindex2:score,...]: output of MahotJob
 *
 * Required args:
 * --inputDir: <string>
 * --appid: <int>
 * --engineid: <int>
 * --algoid: <int>
 * --modelSet: <boolean> (true/false). flag to indicate which set
 * --numSimilarItems: <int>. number of similar items to be generated
 *
 * Optionsl args:
 * --evalid: <int>. Offline Evaluation if evalid is specified
 *
 * Example:
 */
object MahoutModelConstructor {
  /* global */
  val logger = Logger(MahoutModelConstructor.getClass)
  val commonsConfig = new Config

  // argument of this job
  case class JobArg(
    val inputDir: String,
    val appid: Int,
    val algoid: Int,
    val evalid: Option[Int],
    val modelSet: Boolean,
    val numSimilarItems: Int)

  def main(cmdArgs: Array[String]) {
    logger.info("Running model constructor for Mahout ...")
    logger.info(cmdArgs.mkString(","))

    /* get arg */
    val args = Args(cmdArgs)

    val arg = JobArg(
      inputDir = args("inputDir"),
      appid = args("appid").toInt,
      algoid = args("algoid").toInt,
      evalid = args.optional("evalid") map (x => x.toInt),
      modelSet = args("modelSet").toBoolean,
      numSimilarItems = args("numSimilarItems").toInt
    )

    /* run job */
    modelCon(arg)
    cleanUp(arg)
  }

  def modelCon(arg: JobArg) = {

    // NOTE: if OFFLINE_EVAL, write to training modeldata and use evalid as appid
    val OFFLINE_EVAL = (arg.evalid != None)

    val modeldataDb = if (!OFFLINE_EVAL)
      commonsConfig.getModeldataItemSimScores
    else
      commonsConfig.getModeldataTrainingItemSimScores

    val appid = if (OFFLINE_EVAL) arg.evalid.get else arg.appid

    case class ItemData(
      val iid: String,
      val itypes: Seq[String])

    // item index file (iindex iid itypes)
    // iindex -> ItemData
    val itemsMap: Map[Int, ItemData] = Source.fromFile(s"${arg.inputDir}itemsIndex.tsv")
      .getLines()
      .map[(Int, ItemData)] { line =>
        val (iindex, item) = try {
          val fields = line.split("\t")
          val itemData = ItemData(
            iid = fields(1),
            itypes = fields(2).split(",").toList
          )
          (fields(0).toInt, itemData)
        } catch {
          case e: Exception => {
            throw new RuntimeException(s"Cannot get item info in line: ${line}. ${e}")
          }
        }
        (iindex, item)
      }.toMap

    // prediction
    Source.fromFile(s"${arg.inputDir}similarities.tsv")
      .getLines()
      .foreach { line =>
        val fields = line.split("\t")

        val (iindex, predictedData) = try {
          (fields(0).toInt, fields(1))
        } catch {
          case e: Exception => throw new RuntimeException(s"Cannot extract uindex and prediction output from this line: ${line}. ${e}")
        }

        val predicted: Seq[(Int, Double)] = parsePredictedData(predictedData)
          .map { case (iindex, rating) => (iindex.toInt, rating) }

        val topScores = predicted
          // valid item filtering is donw inside MahoutJob
          .sortBy(_._2)(Ordering[Double].reverse)
          .take(arg.numSimilarItems)

        logger.debug(s"${iindex}: ${topScores}")

        val itemid = try {
          itemsMap(iindex).iid
        } catch {
          case e: Exception => throw new RuntimeException(s"Cannot get iid for this iindex: ${line}. ${e}")
        }
        modeldataDb.insert(ItemSimScore(
          iid = itemid,
          simiids = topScores.map(x => itemsMap(x._1).iid),
          scores = topScores.map(_._2),
          itypes = topScores.map(x => itemsMap(x._1).itypes),
          appid = appid,
          algoid = arg.algoid,
          modelset = arg.modelSet))

      }
  }

  def cleanUp(arg: JobArg) = {

  }

  /* TODO refactor this
  Mahout ItemRec output format
  [24:3.2] => (24, 3.2)
  [8:2.5,0:2.5]  => (8, 2.5), (0, 2.5)
  [0:2.0]
  [16:3.0]
  */
  def parsePredictedData(data: String): List[(String, Double)] = {
    val dataLen = data.length
    data.take(dataLen - 1).tail.split(",").toList.map { ratingData =>
      val ratingDataArray = ratingData.split(":")
      val item = ratingDataArray(0)
      val rating: Double = try {
        ratingDataArray(1).toDouble
      } catch {
        case e: Exception =>
          {
            assert(false, s"Cannot convert rating value of item ${item} to double: " + ratingDataArray + ". Exception: " + e)
          }
          0.0
      }
      (item, rating)
    }
  }

}
