package io.prediction.evaluations.scalding.itemrec.trainingtestsplit

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{Users, Items, U2iActions}
import io.prediction.commons.filepath.TrainingTestSplitFile
import io.prediction.commons.appdata.{User, Item}

/**
 * Description:
 *   Split u2i into training and test
 * 
 */
class TrainingTestSplitTime(args: Args) extends TrainingTestSplitCommon(args) {

  val totalArg = args("total").toInt // total u2i count

  val trainingCount: Int = scala.math.ceil((trainingsizeArg.toDouble / totalSize) * totalArg).toInt

  //val testCount: Int = totalArg - trainingCount

  /**
   * source
   */

  // data generated at prep stage
  val u2iSource = U2iActions(appId=evalidArg,
      dbType="file", dbName=TrainingTestSplitFile(hdfsRootArg, appidArg, engineidArg, evalidArg, ""), dbHost=None, dbPort=None)

  /**
   * sink
   */

  val trainingU2iSink = U2iActions(appId=evalidArg,
      dbType=training_dbTypeArg, dbName=training_dbNameArg, dbHost=training_dbHostArg, dbPort=training_dbPortArg)
  
  // sink to test_appadta
  val testU2iSink = U2iActions(appId=evalidArg,
      dbType=test_dbTypeArg, dbName=test_dbNameArg, dbHost=test_dbHostArg, dbPort=test_dbPortArg)

  /**
   * computation
   */

  val sortedU2i = u2iSource.readData('action, 'uid, 'iid, 't, 'v)
    .groupAll( _.sortBy('t) ) // NOTE: small to largest

  sortedU2i.groupAll ( _.take(trainingCount) )
    .then( trainingU2iSink.writeData('action, 'uid, 'iid, 't, 'v, evalidArg) _ ) // NOTE: appid is replaced by evalid

  sortedU2i.groupAll ( _.drop(trainingCount) )
    .then( testU2iSink.writeData('action, 'uid, 'iid, 't, 'v, evalidArg) _ ) // NOTE: appid is replaced by evalid

}
