package io.prediction.metrics.scalding.itemrec.map

import com.twitter.scalding._

import io.prediction.commons.filepath.OfflineMetricFile
import io.prediction.commons.scalding.appdata.U2iActions
//import io.prediction.commons.scalding.modeldata.ItemRecScores

/**
 * Source:
 *   Test set u2iActions.
 *
 * Sink:
 *   relevantItems.tsv eg  u0   i0,i1,i2
 *
 * Description:
 *   Generate relevantItems for MAP@k
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
 * --goalParam: <string> ("view", "conversion", "like", "rate3", "rate4", "rate5)
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
  val GOAL_CONVERSION: String = "conversion"
  val GOAL_LIKE: String = "like"
  val GOAL_RATE3: String = "rate3"
  val GOAL_RATE4: String = "rate4"
  val GOAL_RATE5: String = "rate5"
  val GOAL_ARG_LIST: List[String] = List(GOAL_VIEW, GOAL_CONVERSION, GOAL_LIKE, GOAL_RATE3, GOAL_RATE4, GOAL_RATE5)

  val goalParamArg = args("goalParam")

  require(GOAL_ARG_LIST.contains(goalParamArg), "goalParam " + goalParamArg + " is not valid.")

  val kParamArg = args("kParam").toInt

  /**
   * constants
   */
  final val ACTION_RATE = "rate"
  final val ACTION_LIKE = "like"
  final val ACTION_DISLIKE = "dislike"
  final val ACTION_VIEW = "view"
  //final val ACTION_VIEWDETAILS = "viewDetails"
  final val ACTION_CONVERSION = "conversion"

  /**
   * source
   */
  /*val trainingU2i = U2iActions(appId=evalidArg,
      dbType=training_dbTypeArg, dbName=training_dbNameArg, dbHost=training_dbHostArg, dbPort=training_dbPortArg).readData('actionTrain, 'uidTrain, 'iidTrain, 'tTrain, 'vTrain)*/

  val testU2i = U2iActions(appId = evalidArg,
    dbType = test_dbTypeArg, dbName = test_dbNameArg, dbHost = test_dbHostArg, dbPort = test_dbPortArg).readData('actionTest, 'uidTest, 'iidTest, 'tTest, 'vTest)

  /**
   * computation
   */
  // for each user, get a list of items which match the goalParam
  // TODO: filter out items appeared in trainingU2i?
  val testSetRelevant = testU2i
    .filter('actionTest, 'vTest) { fields: (String, Option[String]) =>
      val (action, v) = fields

      val cond: Boolean = goalParamArg match {
        case GOAL_VIEW => (action == ACTION_VIEW)
        case GOAL_CONVERSION => (action == ACTION_CONVERSION)
        case GOAL_LIKE => (action == ACTION_LIKE)
        case GOAL_RATE3 => try {
          (action == ACTION_RATE) && (v.get.toInt >= 3)
        } catch {
          case e: Exception => {
            assert(false, s"Failed to convert v field ${v} to int. Exception:" + e)
            false
          }
        }
        case GOAL_RATE4 => try {
          (action == ACTION_RATE) && (v.get.toInt >= 4)
        } catch {
          case e: Exception => {
            assert(false, s"Failed to convert v field ${v} to int. Exception:" + e)
            false
          }
        }
        case GOAL_RATE5 => try {
          (action == ACTION_RATE) && (v.get.toInt >= 5)
        } catch {
          case e: Exception => {
            assert(false, s"Failed to convert v field ${v} to int. Exception:" + e)
            false
          }
        }
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

}