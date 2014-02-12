package io.prediction.algorithms.scalding.mahout.itemrec

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Users, Items, U2iActions }
import io.prediction.commons.filepath.DataFile

/**
 * Source:
 *
 * Sink:
 *
 * Descripton:
 *   Prepare data for Mahout itembased Recommendation algo
 *
 * Required args:
 * --dbType: <string> (eg. mongodb) (see --dbHost, --dbPort)
 * --dbName: <string> appdata database name. (eg predictionio_appdata, or predictionio_training_appdata)
 *
 * --hdfsRoot: <string>. Root directory of the HDFS
 *
 * --appid: <int>
 * --engineid: <int>
 * --algoid: <int>
 *
 * --viewParam: <string>. (number 1 to 5, or "ignore")
 * --likeParam: <string>
 * --dislikeParam: <string>
 * --conversionParam: <string>
 * --conflictParam: <string>. (latest/highest/lowest)
 *
 * Optional args:
 * --dbHost: <string> (eg. "127.0.0.1")
 * --dbPort: <int> (eg. 27017)
 *
 * --itypes: <string separated by white space>. eg "--itypes type1 type2". If no --itypes specified, then ALL itypes will be used.
 * --evalid: <int>. Offline Evaluation if evalid is specified
 * --recommendationTime: <long> (eg. 9876543210). generate extra file (recommendItems.csv) which includes items with starttime <= recommendationTime and endtime > recommendationTime
 * --debug: <String>. "test" - for testing purpose
 *
 * Example:
 *
 */
class DataPreparatorCommon(args: Args) extends Job(args) {
  /**
   * parse arguments
   */
  val dbTypeArg = args("dbType")
  val dbNameArg = args("dbName")
  val dbHostArg = args.optional("dbHost")
  val dbPortArg = args.optional("dbPort") map (x => x.toInt) // becomes Option[Int]

  val hdfsRootArg = args("hdfsRoot")

  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = args.optional("evalid") map (x => x.toInt)
  val OFFLINE_EVAL = (evalidArg != None) // offline eval mode

  val preItypesArg = args.list("itypes")
  val itypesArg: Option[List[String]] = if (preItypesArg.mkString(",").length == 0) None else Option(preItypesArg)

  // determine how to map actions to rating values
  def getActionParam(name: String): Option[Int] = {
    val actionParam: Option[Int] = args(name) match {
      case "ignore" => None
      case x => Some(x.toInt)
    }
    actionParam
  }

  val viewParamArg: Option[Int] = getActionParam("viewParam")
  val likeParamArg: Option[Int] = getActionParam("likeParam")
  val dislikeParamArg: Option[Int] = getActionParam("dislikeParam")
  val conversionParamArg: Option[Int] = getActionParam("conversionParam")

  // When there are conflicting actions, e.g. a user gives an item a rating 5 but later dislikes it, 
  // determine which action will be considered as final preference.
  final val CONFLICT_LATEST: String = "latest" // use latest action
  final val CONFLICT_HIGHEST: String = "highest" // use the one with highest score
  final val CONFLICT_LOWEST: String = "lowest" // use the one with lowest score

  val conflictParamArg: String = args("conflictParam")

  // check if the conflictParam is valid
  require(List(CONFLICT_LATEST, CONFLICT_HIGHEST, CONFLICT_LOWEST).contains(conflictParamArg), "conflict param " + conflictParamArg + " is not valid.")

  val debugArg = args.list("debug")
  val DEBUG_TEST = debugArg.contains("test") // test mode

  val recommendationTimeArg = args.optional("recommendationTime").map(_.toLong)

  // NOTE: if OFFLINE_EVAL, read from training set, and use evalid as appid when read Items and U2iActions
  val trainingAppid = if (OFFLINE_EVAL) evalidArg.get else appidArg

}

class DataCopy(args: Args) extends DataPreparatorCommon(args) {

  /**
   * source
   */

  val items = Items(appId = trainingAppid, itypes = itypesArg,
    dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg).readStartEndtime('iidx, 'itypes, 'starttime, 'endtime)

  val users = Users(appId = trainingAppid,
    dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg).readData('uid)

  /**
   * sink
   */
  val userIdSink = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "userIds.tsv"))

  val selectedItemSink = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "selectedItems.tsv"))

  /**
   * computation
   */

  users.write(userIdSink)

  items.mapTo(('iidx, 'itypes, 'starttime, 'endtime) -> ('iidx, 'itypes, 'starttime, 'endtime)) { fields: (String, List[String], Long, Option[Long]) =>
    val (iidx, itypes, starttime, endtime) = fields

    // NOTE: convert List[String] into comma-separated String
    // NOTE: endtime is optional
    (iidx, itypes.mkString(","), starttime, endtime.map(_.toString).getOrElse("PIO_NONE"))
  }.write(selectedItemSink)

}

class DataPreparator(args: Args) extends DataPreparatorCommon(args) {

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

  val u2i = U2iActions(appId = trainingAppid,
    dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg).readData('action, 'uid, 'iid, 't, 'v)

  // use byte offset as index for Mahout algo
  val itemsIndex = TextLine(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "selectedItems.tsv")).read
    .mapTo(('offset, 'line) -> ('iindex, 'iidx, 'itypes, 'starttime, 'endtime)) { fields: (String, String) =>
      val (offset, line) = fields

      val lineArray = line.split("\t")

      val (iidx, itypes, starttime, endtime) = try {
        (lineArray(0), lineArray(1), lineArray(2), lineArray(3))
      } catch {
        case e: Exception => {
          assert(false, "Failed to extract iidx, itypes, starttime and endtime from the line: " + line + ". Exception: " + e)
          (0, "dummy", "dummy", "dummy")
        }
      }

      (offset, iidx, itypes, starttime, endtime)
    }

  val usersIndex = TextLine(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "userIds.tsv")).read
    .rename(('offset, 'line) -> ('uindex, 'uidx))

  /**
   * sink
   */

  val itemsIndexSink = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "itemsIndex.tsv"))

  val usersIndexSink = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "usersIndex.tsv"))

  val ratingsSink = Csv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "ratings.csv"))

  // only recommend these items
  val recommendItemsSink = Csv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "recommendItems.csv"))

  /**
   * computation
   */

  itemsIndex.write(itemsIndexSink)

  usersIndex.write(usersIndexSink)

  // Note: for u2i, use all items of the specified itypes.
  // but recommendItems only include items to be recommended:
  // - with valid starttime and endtime
  recommendationTimeArg.foreach { recTime =>
    itemsIndex
      .filter('starttime, 'endtime) { fields: (Long, String) =>
        val (starttimeI, endtime) = fields

        val endtimeI: Option[Long] = endtime match {
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

        val keepThis: Boolean = (starttimeI, endtimeI) match {
          case (start, None) => (recTime >= start)
          case (start, Some(end)) => ((recTime >= start) && (recTime < end))
          case _ => {
            assert(false, s"Unexpected item starttime ${starttimeI} and endtime ${endtimeI}")
            false
          }
        }
        keepThis
      }
      .project('iindex)
      .write(recommendItemsSink)
  }

  // filter and pre-process actions
  u2i.joinWithSmaller('iid -> 'iidx, itemsIndex) // only select actions of these items
    .filter('action, 'v) { fields: (String, Option[String]) =>
      val (action, v) = fields

      val keepThis: Boolean = action match {
        case ACTION_RATE => true
        case ACTION_LIKE => (likeParamArg != None)
        case ACTION_DISLIKE => (dislikeParamArg != None)
        case ACTION_VIEW => (viewParamArg != None)
        case ACTION_CONVERSION => (conversionParamArg != None)
        case _ => {
          assert(false, "Action type " + action + " in u2iActions appdata is not supported!")
          false // all other unsupported actions
        }
      }
      keepThis
    }
    .map(('action, 'v, 't) -> ('rating, 'tLong)) { fields: (String, Option[String], String) =>
      val (action, v, t) = fields

      // convert actions into rating value based on "action" and "v" fields
      val rating: Int = action match {
        case ACTION_RATE => try {
          v.get.toInt
        } catch {
          case e: Exception => {
            assert(false, s"Failed to convert v field ${v} to integer for ${action} action. Exception:" + e)
            1
          }
        }
        case ACTION_LIKE => likeParamArg.getOrElse {
          assert(false, "Action type " + action + " should have been filtered out!")
          1
        }
        case ACTION_DISLIKE => dislikeParamArg.getOrElse {
          assert(false, "Action type " + action + " should have been filtered out!")
          1
        }
        case ACTION_VIEW => viewParamArg.getOrElse {
          assert(false, "Action type " + action + " should have been filtered out!")
          1
        }
        case ACTION_CONVERSION => conversionParamArg.getOrElse {
          assert(false, "Action type " + action + " should have been filtered out!")
          1
        }
        case _ => { // all other unsupported actions
          assert(false, "Action type " + action + " in u2iActions appdata is not supported!")
          1
        }
      }

      (rating, t.toLong)
    }
    .then(resolveConflict('uid, 'iid, 'tLong, 'rating, conflictParamArg) _)
    .joinWithSmaller('uid -> 'uidx, usersIndex)
    .project('uindex, 'iindex, 'rating)
    .write(ratingsSink) // write ratings to a file

  /**
   * function to resolve conflicting actions of same uid-iid pair.
   */
  def resolveConflict(uidField: Symbol, iidField: Symbol, tfield: Symbol, ratingField: Symbol, conflictSolution: String)(p: RichPipe): RichPipe = {

    // NOTE: sortBy() sort from smallest to largest. use reverse to pick the largest one.
    val dataPipe = conflictSolution match {
      case CONFLICT_LATEST => p.groupBy(uidField, iidField) { _.sortBy(tfield).reverse.take(1) } // take latest one (largest t)
      case CONFLICT_HIGHEST => p.groupBy(uidField, iidField) { _.sortBy(ratingField).reverse.take(1) } // take highest rating
      case CONFLICT_LOWEST => p.groupBy(uidField, iidField) { _.sortBy(ratingField).take(1) } // take lowest rating
    }

    dataPipe
  }

}
