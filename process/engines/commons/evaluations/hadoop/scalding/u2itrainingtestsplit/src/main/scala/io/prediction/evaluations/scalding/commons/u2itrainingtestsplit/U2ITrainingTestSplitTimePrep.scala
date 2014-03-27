package io.prediction.evaluations.scalding.commons.u2itrainingtestsplit

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Users, Items, U2iActions }
import io.prediction.commons.filepath.U2ITrainingTestSplitFile
import io.prediction.commons.appdata.{ User, Item }

/**
 * Description:
 *   Write user/items to db. Read u2iActions and filter with itypes and write to hdfs.
 *   Count number of u2i Actions.
 *   note: appid is replaced by evalid.
 *
 * Args:
 * same as U2ITrainingTestSplitCommon
 */
class U2ITrainingTestSplitTimePrep(args: Args) extends U2ITrainingTestSplitCommon(args) {

  /**
   * source
   */
  // get appdata
  val usersSource = Users(appId = appidArg,
    dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg)

  val itemsSource = Items(appId = appidArg, itypes = itypesArg,
    dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg)

  val u2iSource = U2iActions(appId = appidArg,
    dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg)

  /**
   * sink
   */
  val trainingUsersSink = Users(appId = evalidArg,
    dbType = training_dbTypeArg, dbName = training_dbNameArg, dbHost = training_dbHostArg, dbPort = training_dbPortArg)

  val trainingItemsSink = Items(appId = evalidArg, itypes = None,
    dbType = training_dbTypeArg, dbName = training_dbNameArg, dbHost = training_dbHostArg, dbPort = training_dbPortArg)

  // NOTE: sink to temporary hdfs first
  val u2iSink = U2iActions(appId = evalidArg,
    dbType = "file", dbName = U2ITrainingTestSplitFile(hdfsRootArg, appidArg, engineidArg, evalidArg, ""), dbHost = None, dbPort = None)

  val countSink = Tsv(U2ITrainingTestSplitFile(hdfsRootArg, appidArg, engineidArg, evalidArg, "u2iCount.tsv"))

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
  }.then(trainingItemsSink.writeObj('item) _)

  // write users
  usersSource.readObj('user).mapTo('user -> 'user) { obj: User =>
    val uid = obj.id
    obj.copy(
      id = replacePrefix(uid),
      appid = evalidArg // NOTE: appid is replaced by evalid
    )
  }.then(trainingUsersSink.writeObj('user) _)

  // filter and write u2i
  val itemsIidx = items.mapTo('item -> 'iidx) { obj: Item => obj.id }

  val selectedU2i = u2iSource.readData('action, 'uid, 'iid, 't, 'v)
    .joinWithSmaller('iid -> 'iidx, itemsIidx) // only select actions of these items
    .map(('uid, 'iid, 'action, 'v) -> ('newUid, 'newIid, 'newV)) { fields: (String, String, String, Option[String]) =>

      // NOTE: replace appid prefix by evalid
      val (uid, iid, action, v) = fields
      val newUid = replacePrefix(uid)
      val newIid = replacePrefix(iid)

      /* TODO remove
      // NOTE: add default value 0 for non-rate acitons to work around the optional v field issue 
      //       (cascading-mongo tap can't take optional field yet). 
      val newV = if (v == "") "0" else v */

      (newUid, newIid, v)
    }

  selectedU2i.then(u2iSink.writeData('action, 'newUid, 'newIid, 't, 'newV, evalidArg) _) // NOTE: appid is replaced by evalid 

  // count number of u2i
  selectedU2i.groupAll(_.size('count))
    .write(countSink)

}

