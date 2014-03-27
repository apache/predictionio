package io.prediction.evaluations.scalding.commons.u2itrainingtestsplit

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Users, Items, U2iActions }
import io.prediction.commons.filepath.U2ITrainingTestSplitFile
import io.prediction.commons.appdata.{ User, Item }

/**
 * Description:
 *   TrainingtestsplitCommon
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
 * --validation_dbType: <string> validation_appdata DB type
 * --validation_dbName: <string>
 * --validation_dbHost: <string>. optional
 * --validation_dbPort: <int>. optional
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
 * --trainingPercent: <double> (0.01 to 1). training set percentage
 * --validationPercent: <dboule> (0 to 1). validation set percentage
 * --testPercent: <double> (0.01 to 1). test set percentage
 *
 * --timeorder: <boolean>. Require total percentage < 1
 */
abstract class U2ITrainingTestSplitCommon(args: Args) extends Job(args) {

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

  val validation_dbTypeArg = args("validation_dbType")
  val validation_dbNameArg = args("validation_dbName")
  val validation_dbHostArg = args.optional("validation_dbHost")
  val validation_dbPortArg = args.optional("validation_dbPort") map (x => x.toInt)

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

  val trainingPercentArg = args("trainingPercent").toDouble
  val validationPercentArg = args("validationPercent").toDouble
  val testPercentArg = args("testPercent").toDouble

  val timeorderArg = args("timeorder").toBoolean

  val evaluationPercent = trainingPercentArg + validationPercentArg + testPercentArg

  require(((trainingPercentArg >= 0.01) && (trainingPercentArg <= 1)), "trainingPercent must be >= 0.01 and <= 1.")
  require(((validationPercentArg >= 0) && (validationPercentArg <= 1)), "validationPercent must be >= 0 and <= 1.")
  require(((testPercentArg >= 0.01) && (testPercentArg <= 1)), "testPercent must be >= 0.01 and <= 1.")

  // check valid size
  if (timeorderArg)
    require((evaluationPercent < 1), "The total of training/validation/test must be < 1 if timeorder is true.")
  else
    require((evaluationPercent <= 1), "The total of training/validation/test must be <= 1.")

}
