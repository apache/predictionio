package io.prediction.algorithms.scalding.itemrec.knnitembased

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{Items, U2iActions}
import io.prediction.commons.filepath.DataFile

/**
 * Source: appdata DB (items, u2iActions)
 * Sink: selectedItems.tsv, ratings.tsv
 * Descripton:
 *   Prepare data for itemrec.knnitembased algo. Read from appdata DB and store selected items 
 *   and ratings into a file.
 *   (appdata store -> DataPreparator -> HDFS)
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
 * --viewParam: <int>
 * --likeParam: <int>
 * --dislikeParam: <int>
 * --conversionParam: <int>
 * --conflictParam: <string>. (latest/highest/lowest)
 * 
 * Optional args:
 * --dbHost: <string> (eg. "127.0.0.1")
 * --dbPort: <int> (eg. 27017)
 * 
 * --itypes: <string separated by white space>. eg "--itypes type1 type2". If no --itypes specified, then ALL itypes will be used.
 * --evalid: <int>. Offline Evaluation if evalid is specified
 * --debug: <String>. "test" - for testing purpose
 * 
 * Example:
 * Batch:
 * scald.rb --hdfs-local io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --dbType mongodb --dbName appdata --dbHost 127.0.0.1 --dbPort 27017 --hdfsRoot hdfs/predictionio/ --appid 34 --engineid 3 --algoid 9 --itypes t2 --viewParam 2 --likeParam 5 --dislikeParam 1 --conversionParam 4 --conflictParam latest
 * 
 * Offline Eval:
 * scald.rb --hdfs-local io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --dbType mongodb --dbName training_appdata --dbHost 127.0.0.1 --dbPort 27017 --hdfsRoot hdfs/predictionio/ --appid 34 --engineid 3 --algoid 9 --itypes t2 --viewParam 2 --likeParam 5 --dislikeParam 1 --conversionParam 4 --conflictParam latest --evalid 15
 * 
 */
class DataPreparator(args: Args) extends Job(args) {

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
  
  // determin how to map actions to rating values
  val viewParamArg: Int = args("viewParam").toInt
  val likeParamArg: Int = args("likeParam").toInt
  val dislikeParamArg: Int = args("dislikeParam").toInt
  val conversionParamArg: Int = args("conversionParam").toInt
  //val othersParamArg: Int = args.getOrElse("othersParam", "2").toInt
  
  // When there are conflicting actions, e.g. a user gives an item a rating 5 but later dislikes it, 
  // determine which action will be considered as final preference.
  final val CONFLICT_LATEST: String = "latest" // use latest action
  final val CONFLICT_HIGHEST: String = "highest" // use the one with highest score
  final val CONFLICT_LOWEST: String = "lowest" // use the one with lowest score
  
  val conflictParamArg: String = args("conflictParam")

  // check if the conflictParam is valid
  require(List(CONFLICT_LATEST, CONFLICT_HIGHEST, CONFLICT_LOWEST).contains(conflictParamArg), "conflict param " +conflictParamArg +" is not valid.")

  val debugArg = args.list("debug")
  val DEBUG_TEST = debugArg.contains("test") // test mode
  
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
  // get appdata
  // NOTE: if OFFLINE_EVAL, read from training set, and use evalid as appid when read Items and U2iActions
  val trainingAppid = if (OFFLINE_EVAL) evalidArg.get else appidArg 
  
  // get items data
  val items = Items(appId=trainingAppid, itypes=itypesArg, 
      dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg).readData('iidx, 'itypes)
  
  val u2i = U2iActions(appId=trainingAppid, 
      dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg).readData('action, 'uid, 'iid, 't, 'v)

  /**
   * computation
   */
  u2i.joinWithSmaller('iid -> 'iidx, items) // only select actions of these items
    .map(('action, 'v, 't) -> ('rating, 'tLong)) { fields: (String, String, String) =>
      val (action, v, t) = fields
      
      // convert actions into rating value based on "action" and "v" fields
      val rating: Int = action.toInt match {
        case ACTION_RATE => v.toInt // rate
        case ACTION_LIKEDISLIKE => if (v.toInt == 1) likeParamArg else dislikeParamArg
        case ACTION_VIEW => viewParamArg // view
        case ACTION_CONVERSION => conversionParamArg // conversion
        case _ => {
          assert(false, "Action type " + action.toInt + " in u2iActions appdata is not supported in itemrec.knnitembased.DataPreparator!")
          1
          } //othersParamArg // all other unsupported actions
      }
      
      (rating, t.toLong)
    } 
    .then( resolveConflict('uid, 'iid, 'tLong, 'rating, conflictParamArg) _ )
    .project('uid, 'iid, 'rating)
    .write(Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "ratings.tsv"))) // write ratings to a file
 
  // Also store the selected items into DataFile for later model construction usage.
  items.mapTo(('iidx, 'itypes) -> ('iidx, 'itypes)) { fields: (String, List[String]) =>
    val (iidx, itypes) = fields
    
    (iidx, itypes.mkString(",")) // NOTE: convert List[String] into comma-separated String
    }.write(Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "selectedItems.tsv")))
  
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
