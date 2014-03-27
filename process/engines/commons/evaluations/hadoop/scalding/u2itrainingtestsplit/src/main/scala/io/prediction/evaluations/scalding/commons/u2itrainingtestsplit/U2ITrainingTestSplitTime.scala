package io.prediction.evaluations.scalding.commons.u2itrainingtestsplit

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Users, Items, U2iActions }
import io.prediction.commons.filepath.U2ITrainingTestSplitFile
import io.prediction.commons.appdata.{ User, Item }

/**
 * Description:
 *   Split u2i into training, validation and test set
 *
 * Args:
 * same as TrainingtestsplitCommon, plus additional args:
 * --totalCount <int> total u2i actions count
 */
class U2ITrainingTestSplitTime(args: Args) extends U2ITrainingTestSplitCommon(args) {

  val totalCountArg = args("totalCount").toInt // total u2i count

  // evaluationPercent is sum of trainingPercentArg + validationPercentArg + testPercentArg
  val evaluationCount: Int = (scala.math.floor(evaluationPercent * totalCountArg)).toInt

  val trainingCount: Int = (scala.math.floor(trainingPercentArg * totalCountArg)).toInt
  val validationCount: Int = (scala.math.floor(validationPercentArg * totalCountArg)).toInt

  val trainingValidationCount: Int = trainingCount + validationCount
  val testCount = evaluationCount - trainingValidationCount

  require((trainingCount >= 1), "Not enough data for training set. trainingCount = " + trainingCount)
  if (validationPercentArg != 0) {
    require((validationCount >= 1), "Not enough data for validation set. validationCount = " + validationCount)
  }
  require((testCount >= 1), "Not enough data for test set. testCount = " + testCount)

  /**
   * source
   */

  // data generated at prep stage
  val u2iSource = U2iActions(appId = evalidArg,
    dbType = "file", dbName = U2ITrainingTestSplitFile(hdfsRootArg, appidArg, engineidArg, evalidArg, ""), dbHost = None, dbPort = None)

  /**
   * sink
   */

  val trainingU2iSink = U2iActions(appId = evalidArg,
    dbType = training_dbTypeArg, dbName = training_dbNameArg, dbHost = training_dbHostArg, dbPort = training_dbPortArg)

  val validationU2iSink = U2iActions(appId = evalidArg,
    dbType = validation_dbTypeArg, dbName = validation_dbNameArg, dbHost = validation_dbHostArg, dbPort = validation_dbPortArg)

  // sink to test_appadta
  val testU2iSink = U2iActions(appId = evalidArg,
    dbType = test_dbTypeArg, dbName = test_dbNameArg, dbHost = test_dbHostArg, dbPort = test_dbPortArg)

  /**
   * computation
   */

  val randomU2i = if (timeorderArg) {

    // shuffle, take and then sort
    u2iSource.readData('action, 'uid, 'iid, 't, 'v)
      .shuffle(11)
      .groupAll(_.take(evaluationCount))
      .groupAll(_.sortBy('t)) // NOTE: small to largest (oldest first, so training set should be taken first)

  } else {

    // shuffle and then take
    u2iSource.readData('action, 'uid, 'iid, 't, 'v)
      .shuffle(11)
      .groupAll(_.take(evaluationCount))

  }

  // split
  val trainingOrValidation = randomU2i.groupAll(_.take(trainingValidationCount))

  trainingOrValidation.groupAll(_.take(trainingCount))
    .then(trainingU2iSink.writeData('action, 'uid, 'iid, 't, 'v, evalidArg) _) // NOTE: appid is replaced by evalid

  trainingOrValidation.groupAll(_.drop(trainingCount))
    .then(validationU2iSink.writeData('action, 'uid, 'iid, 't, 'v, evalidArg) _) // NOTE: appid is replaced by evalid

  randomU2i.groupAll(_.drop(trainingValidationCount))
    .then(testU2iSink.writeData('action, 'uid, 'iid, 't, 'v, evalidArg) _) // NOTE: appid is replaced by evalid

}
