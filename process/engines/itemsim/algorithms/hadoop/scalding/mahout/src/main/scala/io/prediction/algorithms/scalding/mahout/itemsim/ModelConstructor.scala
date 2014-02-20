package io.prediction.algorithms.scalding.mahout.itemsim

import com.twitter.scalding._

import io.prediction.commons.filepath.{ DataFile, AlgoFile }
import io.prediction.commons.scalding.modeldata.ItemSimScores

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
 * --numSimilarItems: <int>. number of similar items to be generated
 * --recommendationTime: <long> (eg. 9876543210). recommend items with starttime <= recommendationTime and endtime > recommendationTime
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
  val dbHostArg = args.optional("dbHost")
  val dbPortArg = args.optional("dbPort") map (x => x.toInt)

  val hdfsRootArg = args("hdfsRoot")

  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = args.optional("evalid") map (x => x.toInt)
  val OFFLINE_EVAL = (evalidArg != None) // offline eval mode

  val debugArg = args.list("debug")
  val DEBUG_TEST = debugArg.contains("test") // test mode

  val modelSetArg = args("modelSet").toBoolean

  val numSimilarItems = args("numSimilarItems").toInt
  val recommendationTimeArg = args("recommendationTime").toLong

  /**
   * source
   */
  val similarities = Tsv(AlgoFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "similarities.tsv"), ('iindex, 'simiindex, 'score)).read
    .mapTo(('iindex, 'simiindex, 'score) -> ('iindex, 'simiindex, 'score)) {
      fields: (String, String, Double) => fields // convert score from String to Double
    }

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

  /**
   * sink
   */

  val ItemSimScoresSink = ItemSimScores(dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg, algoid = algoidArg, modelset = modelSetArg)

  /**
   * computation
   */
  val sim = similarities.joinWithSmaller('iindex -> 'iindexI, itemsIndex)
    .discard('iindex, 'iindexI)
    .rename(('iidI, 'itypesI, 'starttimeI, 'endtimeI) -> ('iid, 'itypes, 'starttime, 'endtime))
    .joinWithSmaller('simiindex -> 'iindexI, itemsIndex)

  // NOTE: use simiid's starttime and endtime. not iid's.
  val sim1 = sim.project('iid, 'iidI, 'itypesI, 'score, 'starttimeI, 'endtimeI)
  // NOTE: mahout only calculate half of the sim matrix, reverse the fields to get the other half
  val sim2 = sim.mapTo(('iidI, 'iid, 'itypes, 'score, 'starttime, 'endtime) -> ('iid, 'iidI, 'itypesI, 'score, 'starttimeI, 'endtimeI)) {
    fields: (String, String, List[String], Double, Long, Option[Long]) => fields
  }

  val combinedSimilarities = sim1 ++ sim2

  combinedSimilarities
    .filter('starttimeI, 'endtimeI) { fields: (Long, Option[Long]) =>
      val (starttimeI, endtimeI) = fields

      val keepThis: Boolean = (starttimeI, endtimeI) match {
        case (start, None) => (recommendationTimeArg >= start)
        case (start, Some(end)) => ((recommendationTimeArg >= start) && (recommendationTimeArg < end))
        case _ => {
          assert(false, s"Unexpected item starttime ${starttimeI} and endtime ${endtimeI}")
          false
        }
      }
      keepThis
    }
    .groupBy('iid) { _.sortBy('score).reverse.toList[(String, Double, List[String])](('iidI, 'score, 'itypesI) -> 'simiidsList) }
    .mapTo(('iid, 'simiidsList) -> ('iid, 'simiidsList)) { fields: (String, List[(String, Double, List[String])]) =>
      val (iid, simiidsList) = fields

      (iid, simiidsList.take(numSimilarItems))
    }
    .then(ItemSimScoresSink.writeData('iid, 'simiidsList, algoidArg, modelSetArg) _)

}
