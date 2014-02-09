package io.prediction.algorithms.scalding.itemrec.randomrank

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Items, Users }
import io.prediction.commons.scalding.modeldata.ItemRecScores
import io.prediction.commons.filepath.{ AlgoFile }

/**
 * Source:
 *
 * Sink:
 *
 * Description:
 *
 * Args:
 * --training_dbType: <string> training_appdata DB type
 * --training_dbName: <string>
 * --training_dbHost: <string> optional
 * --training_dbPort: <int> optional
 *
 * --modeldata_dbType: <string> modeldata DB type
 * --modeldata_dbName: <string>
 * --modeldata_dbHost: <string> optional
 * --modeldata_dbPort <int> optional
 *
 * --hdfsRoot: <string>. Root directory of the HDFS
 *
 * --appid: <int>
 * --engineid: <int>
 * --algoid: <int>
 * --evalid: <int>. optional. Offline Evaluation if evalid is specified
 *
 * --itypes: <string separated by white space>. optional. eg "--itypes type1 type2". If no --itypes specified, then ALL itypes will be used.
 * --numRecommendations: <int>. number of recommendations to be generated
 * --modelSet: <boolean> (true/false). flag to indicate which set
 * --recommendationTime: <long> (eg. 9876543210). recommend items with starttime <= recommendationTime and endtime > recommendationTime
 *
 * Example:
 * hadoop jar PredictionIO-Process-Hadoop-Scala-assembly-0.1.jar io.prediction.algorithms.scalding.itemrec.randomrank.RandomRank --hdfs --training_dbType mongodb --training_dbName predictionio_appdata --training_dbHost localhost --training_dbPort 27017 --modeldata_dbType mongodb --modeldata_dbName predictionio_modeldata --modeldata_dbHost localhost --modeldata_dbPort 27017 --hdfsRoot predictionio/ --appid 1 --engineid 1 --algoid 18 --modelSet true
 */
class RandomRank(args: Args) extends Job(args) {

  /**
   * parse args
   */
  val training_dbTypeArg = args("training_dbType")
  val training_dbNameArg = args("training_dbName")
  val training_dbHostArg = args.optional("training_dbHost")
  val training_dbPortArg = args.optional("training_dbPort") map (x => x.toInt)

  val modeldata_dbTypeArg = args("modeldata_dbType")
  val modeldata_dbNameArg = args("modeldata_dbName")
  val modeldata_dbHostArg = args.optional("modeldata_dbHost")
  val modeldata_dbPortArg = args.optional("modeldata_dbPort") map (x => x.toInt)

  val hdfsRootArg = args("hdfsRoot")

  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = args.optional("evalid") map (x => x.toInt)
  val OFFLINE_EVAL = (evalidArg != None) // offline eval mode

  val preItypesArg = args.list("itypes")
  val itypesArg: Option[List[String]] = if (preItypesArg.mkString(",").length == 0) None else Option(preItypesArg)

  val numRecommendationsArg = args("numRecommendations").toInt

  val modelSetArg = args("modelSet").toBoolean
  val recommendationTimeArg = args("recommendationTime").toLong

  /**
   * source
   */

  // get appdata
  // NOTE: if OFFLINE_EVAL, read from training set, and use evalid as appid when read Items and U2iActions
  val trainingAppid = if (OFFLINE_EVAL) evalidArg.get else appidArg

  // get items data
  val items = Items(appId = trainingAppid, itypes = itypesArg,
    dbType = training_dbTypeArg, dbName = training_dbNameArg, dbHost = training_dbHostArg, dbPort = training_dbPortArg).readStartEndtime('iidx, 'itypes, 'starttime, 'endtime)

  val users = Users(appId = trainingAppid,
    dbType = training_dbTypeArg, dbName = training_dbNameArg, dbHost = training_dbHostArg, dbPort = training_dbPortArg).readData('uid)

  // TODO: unseenOnly filtering (need u2iActions)

  /**
   * sink
   */
  val itemRecScores = ItemRecScores(dbType = modeldata_dbTypeArg, dbName = modeldata_dbNameArg, dbHost = modeldata_dbHostArg, dbPort = modeldata_dbPortArg, algoid = algoidArg, modelset = modelSetArg)

  //val scoresFile = Tsv(AlgoFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "itemRecScores.tsv"))

  /**
   * computation
   */
  val itemsWithKey = items
    .filter('starttime, 'endtime) { fields: (Long, Option[Long]) =>
      // only keep items with valid starttime and endtime
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
    .map(() -> 'itemKey) { u: Unit => 1 }

  val usersWithKey = users.map(() -> 'userKey) { u: Unit => 1 }

  val scores = usersWithKey.joinWithSmaller('userKey -> 'itemKey, itemsWithKey)
    .map(() -> 'score) { u: Unit => scala.util.Random.nextDouble() }
    .project('uid, 'iidx, 'score, 'itypes)
    .groupBy('uid) { _.sortBy('score).reverse.take(numRecommendationsArg) }
    // another way to is to do toList then take top n from List. But then it would create an unncessary long List
    // for each group first. not sure which way is better.
    .groupBy('uid) { _.sortBy('score).reverse.toList[(String, Double, List[String])](('iidx, 'score, 'itypes) -> 'iidsList) }
    .then(itemRecScores.writeData('uid, 'iidsList, algoidArg, modelSetArg) _)

}
