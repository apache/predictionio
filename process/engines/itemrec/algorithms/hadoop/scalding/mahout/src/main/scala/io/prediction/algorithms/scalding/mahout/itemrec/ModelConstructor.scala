package io.prediction.algorithms.scalding.mahout.itemrec

import com.twitter.scalding._

import io.prediction.commons.filepath.{ DataFile, AlgoFile }
import io.prediction.commons.scalding.modeldata.ItemRecScores
import cascading.pipe.joiner.LeftJoin

/**
 * Source:
 *
 * Sink:
 *
 * Description:
 *
 * Required args:
 * --dbType: <string> modeldata DB type (eg. mongodb) (see --dbHost, --dbPort)
 * --dbName: <string> (eg. predictionio_modeldata)
 *
 * --hdfsRoot: <string>. Root directory of the HDFS
 *
 * --appid: <int>
 * --engineid: <int>
 * --algoid: <int>
 * --modelSet: <boolean> (true/false). flag to indicate which set
 *
 * --numRecommendations: <int>. number of recommendations to be generated
 *
 * Optionsl args:
 * --dbHost: <string> (eg. "127.0.0.1")
 * --dbPort: <int> (eg. 27017)
 *
 * --evalid: <int>. Offline Evaluation if evalid is specified
 * --debug: <String>. "test" - for testing purpose
 *
 * Example:
 *
 */
class ModelConstructor(args: Args) extends Job(args) {

  /**
   * parse args
   */
  val dbTypeArg = args("dbType")
  val dbNameArg = args("dbName")
  val dbHostArg = args.list("dbHost")
  val dbPortArg = args.list("dbPort") map (x => x.toInt)

  val hdfsRootArg = args("hdfsRoot")

  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = args.optional("evalid") map (x => x.toInt)
  val OFFLINE_EVAL = (evalidArg != None) // offline eval mode

  val debugArg = args.list("debug")
  val DEBUG_TEST = debugArg.contains("test") // test mode

  val modelSetArg = args("modelSet").toBoolean

  val numRecommendationsArg = args("numRecommendations").toInt

  /**
   * source
   */

  val predicted = Tsv(AlgoFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "predicted.tsv"), ('uindex, 'predicted)).read

  val itemsIndex = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "itemsIndex.tsv")).read
    .mapTo((0, 1, 2, 3, 4) -> ('iindexI, 'iidI, 'itypesI, 'starttimeI, 'endtimeI)) { fields: (String, String, String, Long, String) =>
      val (iindex, iid, itypes, starttime, endtime) = fields // itypes are comma-separated String

      val endtimeOpt: Option[Long] = endtime match {
        case "PIO_NONE" => None
        case x: String => {
          try {
            Some(x.toLong)
          } catch {
            case e: Exception => {
              assert(false, s"Failed to convert ${x} to Long. Exception: " + e)
              Some(0)
            }
          }
        }
      }

      (iindex, iid, itypes.split(",").toList, starttime, endtimeOpt)
    }

  val usersIndex = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "usersIndex.tsv")).read
    .mapTo((0, 1) -> ('uindexU, 'uidU)) { fields: (String, String) =>

      fields
    }

  /**
   * sink
   */

  val itemRecScoresSink = ItemRecScores(dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg, algoid = algoidArg, modelset = modelSetArg)

  /**
   * computation
   */
  // convert to (uindex, iindex, rating) format
  val predictedRating = predicted.flatMap('predicted -> ('iindex, 'rating)) { data: String => parsePredictedData(data) }
    .project('uindex, 'iindex, 'rating)

  predictedRating
    .joinWithSmaller('iindex -> 'iindexI, itemsIndex)
    .joinWithSmaller('uindex -> 'uindexU, usersIndex)
    .project('uidU, 'iidI, 'rating, 'itypesI)
    .groupBy('uidU) { _.sortBy('rating).reverse.toList[(String, Double, List[String])](('iidI, 'rating, 'itypesI) -> 'iidsList) }
    .mapTo(('uidU, 'iidsList) -> ('uidU, 'iidsList)) {
      fields: (String, List[(String, Double, List[String])]) =>
        val (uidU, iidsList) = fields
        (uidU, iidsList.take(numRecommendationsArg))
    }
    .then(itemRecScoresSink.writeData('uidU, 'iidsList, algoidArg, modelSetArg) _)

  /*
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
