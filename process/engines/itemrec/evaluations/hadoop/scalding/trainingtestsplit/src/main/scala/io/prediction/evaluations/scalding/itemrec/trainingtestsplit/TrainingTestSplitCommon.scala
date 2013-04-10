package io.prediction.evaluations.scalding.itemrec.trainingtestsplit

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{Users, Items, U2iActions}
import io.prediction.commons.filepath.TrainingTestSplitFile
import io.prediction.commons.appdata.{User, Item}

/**
 * Description:
 *   trainingtestsplittime Common
 *
 * Args: 
 * --dbType: <string> appdata DB type
 * --dbName: <string>
 * --dbHost: <string>. optional. (eg. "127.0.0.1")
 * --dbPort: <int>. optional. (eg. 27017)
 *
 * --training_dbType: <string> training_appadta DB type
 * --training_dbName: <string>
 * --training_dbHost: <string>. optional
 * --training_dbPort: <int>. optional
 * 
 * --test_dbType: <string> test_appdata DB type
 * --test_dbName: <string>
 * --test_dbHost: <string>. optional
 * --test_dbPort: <int>. optional
 *
 * --hdfsRoot: <string>. Root directory of the HDFS
 * 
 * --appid: <int>
 * --engineid: <int>
 * --evalid: <int>
 *
 * --itypes: <string separated by white space>. eg "--itypes type1 type2". If no --itypes specified, then ALL itypes will be used.
 *
 * --trainingsize: <int> (1 - 10)
 * --testsize: <int> (1 - 10)
 * --timeorder: <boolean>. Require total size < 10. 
 */
abstract class TrainingTestSplitCommon(args: Args) extends Job(args) {

  /**
   * parse arguments
   */
  val dbTypeArg = args("dbType")
  val dbNameArg = args("dbName")
  val dbHostArg = args.optional("dbHost")
  val dbPortArg = args.optional("dbPort") map (x => x.toInt) 
  
  val training_dbTypeArg = args("training_dbType")
  val training_dbNameArg = args("training_dbName")
  val training_dbHostArg = args.optional("training_dbHost")
  val training_dbPortArg = args.optional("training_dbPort") map (x => x.toInt) 
  
  val test_dbTypeArg = args("test_dbType")
  val test_dbNameArg = args("test_dbName")
  val test_dbHostArg = args.optional("test_dbHost")
  val test_dbPortArg = args.optional("test_dbPort") map (x => x.toInt) 

  val hdfsRootArg = args("hdfsRoot")
  
  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val evalidArg = args("evalid").toInt
  
  val preItypesArg = args.list("itypes")
  val itypesArg: Option[List[String]] = if (preItypesArg.mkString(",").length == 0) None else Option(preItypesArg)

  val trainingsizeArg = args("trainingsize").toInt
  val testsizeArg = args("testsize").toInt
  val timeorderArg = args("timeorder").toBoolean

  val totalSize = trainingsizeArg + testsizeArg

  // check valid size
  if (timeorderArg)
    require((totalSize < 10), "The total of trainingsize and testsize must be < 10 if timeorder is true.")
  else
    require((totalSize <= 10), "The total of trainingsize and testsize must be <= 10.")

}
