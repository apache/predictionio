package io.prediction.metrics.scalding.itemrec.map

import com.twitter.scalding._

import io.prediction.commons.filepath.OfflineMetricFile
import io.prediction.commons.scalding.appdata.U2iActions
//import io.prediction.commons.scalding.modeldata.ItemRecScores

/**
 * Source:
 *   Test set u2iActions.
 *   ItemRecScores
 *
 * Sink:
 *   relevantItems.tsv eg  u0   i0,i1,i2
 *   topKItems.tsv  eg.  u0  i1,i4,i5
 *
 * Description:
 *   Generate relevantItems and topKItems for MAP@k
 *
 * Required args:
 * --test_dbType: <string> test_appdata DB type (eg. mongodb)
 * --test_dbName: <string>
 *
 * --training_dbType: <string> training_appdata DB type
 * --training_dbName: <string>
 *
 * --modeldata_dbType: <string> modeldata DB type
 * --modeldata_dbName: <string>
 *
 * --hdfsRoot: <string>. Root directory of the HDFS
 *
 * --appid: <int>
 * --engineid: <int>
 * --evalid: <int>
 * --metricid: <int>
 * --algoid: <int>
 *
 * --kParam: <int>
 * --goalParam: <string> ("view", "buy", "like", "rate3", "rate4", "rate5)
 *
 * Optional args:
 * --test_dbHost: <string> (eg. "127.0.0.1")
 * --test_dbPort: <int> (eg. 27017)
 *
 * --training_dbHost: <string>
 * --training_dbPort: <int>
 *
 * --modeldata_dbHost: <string>
 * --modeldata_dbPort <int>
 *
 * --debug: <String>. "test" - for testing purpose
 *
 * Example:
 * scald.rb --hdfs-local io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --test_dbType mongodb --test_dbName test_appdata --test_dbHost 127.0.0.1 --test_dbPort 27017 --training_dbType mongodb --training_dbName training_appdata --training_dbHost 127.0.0.1 --training_dbPort 27017 --modeldata_dbType file --modeldata_dbName modeldata_path/ --hdfsRoot hdfs/predictionio/ --appid 34 --engineid 3 --evalid 15 --metricid 10 --algoid 9 --kParam 30 --goalParam rate3
 *
 */
class MAPAtKDataPreparator(args: Args) extends Job(args) {

  /**
   * parse arguments
   */
  val test_dbTypeArg = args("test_dbType")
  val test_dbNameArg = args("test_dbName")
  val test_dbHostArg = args.optional("test_dbHost")
  val test_dbPortArg = args.optional("test_dbPort") map (x => x.toInt)

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
  val evalidArg = args("evalid").toInt
  val metricidArg = args("metricid").toInt
  val algoidArg = args("algoid").toInt

  val GOAL_VIEW: String = "view"
  val GOAL_BUY: String = "buy"
  val GOAL_LIKE: String = "like"
  val GOAL_RATE3: String = "rate3"
  val GOAL_RATE4: String = "rate4"
  val GOAL_RATE5: String = "rate5"
  val GOAL_ARG_LIST: List[String] = List(GOAL_VIEW, GOAL_BUY, GOAL_LIKE, GOAL_RATE3, GOAL_RATE4, GOAL_RATE5)

  val goalParamArg = args("goalParam")

  require(GOAL_ARG_LIST.contains(goalParamArg), "goalParam " + goalParamArg + " is not valid.")

  val kParamArg = args("kParam").toInt

  /**
   * constants
   */
  // NOTE: this enum should match the assumption of appdata
  // see api/model/U2iAction.scala
  final val ACTION_RATE: Int = 0
  final val ACTION_LIKEDISLIKE: Int = 1 // if field "v"==1, then Like. "v"==0, then Dislike
  final val ACTION_VIEW: Int = 2
  //final val ACTION_VIEWDETAILS: Int = 3
  final val ACTION_CONVERSION: Int = 4

  /**
   * source
   */
  /*val trainingU2i = U2iActions(appId=evalidArg,
      dbType=training_dbTypeArg, dbName=training_dbNameArg, dbHost=training_dbHostArg, dbPort=training_dbPortArg).readData('actionTrain, 'uidTrain, 'iidTrain, 'tTrain, 'vTrain)*/

  val testU2i = U2iActions(appId=evalidArg,
      dbType=test_dbTypeArg, dbName=test_dbNameArg, dbHost=test_dbHostArg, dbPort=test_dbPortArg).readData('actionTest, 'uidTest, 'iidTest, 'tTest, 'vTest)

  /**
  val itemRecScores = ItemRecScores(dbType=modeldata_dbTypeArg, dbName=modeldata_dbNameArg, dbHost=modeldata_dbHostArg, dbPort=modeldata_dbPortArg)
    .readData('uid, 'iid, 'score, 'itypes)
    */

  /**
   * computation
   */
  // for each user, get a list of items which match the goalParam
  // TODO: filter out items appeared in trainingU2i?
  val testSetRelevant = testU2i
    .filter('actionTest, 'vTest) { fields: (String, String) =>
      val (action, v) = fields

      val cond: Boolean = goalParamArg match {
        case GOAL_VIEW => (action.toInt == ACTION_VIEW)
        case GOAL_BUY => (action.toInt == ACTION_CONVERSION)
        case GOAL_LIKE => (action.toInt == ACTION_LIKEDISLIKE) && (v.toInt == 1)
        case GOAL_RATE3 => (action.toInt == ACTION_RATE) && (v.toInt >= 3)
        case GOAL_RATE4 => (action.toInt == ACTION_RATE) && (v.toInt >= 4)
        case GOAL_RATE5 => (action.toInt == ACTION_RATE) && (v.toInt >= 5)
        case _ => {
          assert(false, "Invalid goalParam " + goalParamArg + ".")
          false
        }
      }
      cond
    }
    .groupBy('uidTest) { _.toList[String]('iidTest -> 'relevantList) }
    .mapTo(('uidTest, 'relevantList) -> ('uidTest, 'relevantList)) { fields: (String, List[String]) =>
      val (uidTest, relevantList) = fields

      (uidTest, relevantList.mkString(","))
    }
    .write(Tsv(OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "relevantItems.tsv")))

  // retreive top-K items from itemRecScores for each user
  //val topKItems = itemRecScores
    // NOTE: sortBy is from small to large. so need to do reverse since higher score means better.
    /**
    .groupBy('uid) { _.sortBy('score).reverse.take(kParamArg) }
    .groupBy('uid) { _.toList[String]('iid -> 'topListTemp)}
      */
    // NOTE: the _.toList group method would create a list of iids in reverse order of the score
    //  (the highest score item is the end of list).
    //  so need to reverse the order back again so that highest score iid is first item in the list
    /**
    .mapTo(('uid, 'topListTemp) -> ('uid, 'topList)) { fields: (String, List[String]) =>
      val (uid, topListTemp) = fields

      (uid, topListTemp.reverse.mkString(","))
    }
    .write(Tsv(OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "topKItems.tsv")))
      */
}