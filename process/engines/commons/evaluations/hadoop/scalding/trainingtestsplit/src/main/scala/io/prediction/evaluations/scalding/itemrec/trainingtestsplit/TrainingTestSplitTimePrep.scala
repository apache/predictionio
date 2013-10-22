package io.prediction.evaluations.scalding.itemrec.trainingtestsplit

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{Users, Items, U2iActions}
import io.prediction.commons.filepath.TrainingTestSplitFile
import io.prediction.commons.appdata.{User, Item}

/**
 * Description:
 *   Write user/items to db. Read u2iActions and filter with itypes and write to hdfs.
 *   Count number of u2i Actions.
 *   note: appid is replaced by evalid.
 * 
 * Args:
 * same as TrainingTestSplitCommon
 */
class TrainingTestSplitTimePrep(args: Args) extends TrainingTestSplitCommon(args) {

  /**
   * source
   */
  // get appdata
  val usersSource = Users(appId=appidArg,
      dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg)

  val itemsSource = Items(appId=appidArg, itypes=itypesArg,
      dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg)

  val u2iSource = U2iActions(appId=appidArg,
      dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg)

  /**
   * sink
   */
  val trainingUsersSink = Users(appId=evalidArg,
      dbType=training_dbTypeArg, dbName=training_dbNameArg, dbHost=training_dbHostArg, dbPort=training_dbPortArg)

  val trainingItemsSink = Items(appId=evalidArg, itypes=None, 
      dbType=training_dbTypeArg, dbName=training_dbNameArg, dbHost=training_dbHostArg, dbPort=training_dbPortArg)

  // NOTE: sink to temporary hdfs first
  val u2iSink = U2iActions(appId=evalidArg,
      dbType="file", dbName=TrainingTestSplitFile(hdfsRootArg, appidArg, engineidArg, evalidArg, ""), dbHost=None, dbPort=None)

  val countSink = Tsv(TrainingTestSplitFile(hdfsRootArg, appidArg, engineidArg, evalidArg, "u2iCount.tsv"))
  
  /**
   * computation
   */

  val oldPrefix: String = appidArg + "_"
  val newPrefix: String = evalidArg + "_"
  
  def replacePrefix(org: String): String = newPrefix + org.stripPrefix(oldPrefix)

  val items = itemsSource.readObj('item)

  // write items
  items.mapTo('item -> 'item) { obj: Item =>
    val iid = obj.id
    obj.copy(
      id = replacePrefix(iid),
      appid = evalidArg // NOTE: appid is replaced by evalid
    )
  }.then( trainingItemsSink.writeObj('item) _ )  
  
  // write users
  usersSource.readObj('user).mapTo('user -> 'user) { obj: User =>
    val uid = obj.id
    obj.copy(
      id = replacePrefix(uid),
      appid = evalidArg // NOTE: appid is replaced by evalid
    )
  }.then( trainingUsersSink.writeObj('user) _ )

  // filter and write u2i
  val itemsIidx = items.mapTo('item -> 'iidx) { obj: Item => obj.id }
  
  val selectedU2i = u2iSource.readData('action, 'uid, 'iid, 't, 'v)
    .joinWithSmaller('iid -> 'iidx, itemsIidx) // only select actions of these items
    .map(('uid, 'iid) -> ('newUid, 'newIid)) { fields: (String, String) =>
      
      // NOTE: replace appid prefix by evalid
      val (uid, iid) = fields
      val newUid = replacePrefix(uid)
      val newIid = replacePrefix(iid)
      
      (newUid, newIid)
    }

  selectedU2i.then( u2iSink.writeData('action, 'newUid, 'newIid, 't, 'v, evalidArg) _ ) // NOTE: appid is replaced by evalid 
  
  // count number of u2i
  selectedU2i.groupAll( _.size('count) )
    .write(countSink)

}

