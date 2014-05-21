package io.prediction.engines.itemrank

import io.prediction.storage.Config
import io.prediction.storage.{ User, Item, U2IAction, ItemSet }

import scala.util.Random
import grizzled.slf4j.Logger
import com.github.nscala_time.time.Imports._

object SampleData {

  val logger = Logger(SampleData.getClass)
  val config = new Config
  val rand = new Random(0) // random with seed

  val usersDb = config.getAppdataUsers
  val itemsDb = config.getAppdataItems
  val u2iDb = config.getAppdataU2IActions
  val itemSetsDb = config.getAppdataItemSets

  def createSampleData(appid: Int) = {

    // remove old data if exists
    usersDb.deleteByAppid(appid)
    itemsDb.deleteByAppid(appid)
    u2iDb.deleteByAppid(appid)
    itemSetsDb.deleteByAppid(appid)

    val userIds = Range(0, 10).map(i => s"u${i}").toList
    val itemIds = Range(0, 100).map(i => s"i${i}").toList

    val refTime = new DateTime("2014-04-01T06:10:51.754-07:00")
    val itemSetIds = Range(0, 30).toList.map { d =>
      (s"s${d}", refTime + d.days)
    }
    // each user has higher pref on these 20 items
    val userPref = userIds.map { u =>
      (u, rand.shuffle(itemIds).take(20))
    }
    // there are 10 items to be ranked in each days
    val itemOfEachDay = itemSetIds.map {
      case (sid, d) =>
        (sid, d, rand.shuffle(itemIds).take(10))
    }

    // create u2i if the items have the item the user like
    itemOfEachDay.map {
      case (sid, t, listOfItems) =>
        itemSetsDb.insert(ItemSet(
          id = sid,
          appid = appid,
          iids = listOfItems,
          t = Some(t)
        ))
        userPref.foreach {
          case (uid, prefItems) =>
            // assume the user only acts on the 1st liked item
            val actedIids = (listOfItems intersect prefItems).take(1)
            actedIids.foreach { iid =>
              u2iDb.insert(U2IAction(
                appid = appid,
                action = "conversion",
                uid = uid,
                iid = iid,
                t = t
              ))
            }
        }
    }

    userIds.foreach { uid =>
      usersDb.insert(User(
        id = uid,
        appid = appid,
        ct = refTime
      ))
    }

    itemIds.foreach { iid =>
      itemsDb.insert(Item(
        id = iid,
        appid = appid,
        ct = DateTime.now,
        itypes = Seq("t1", "t2"),
        starttime = Some(refTime),
        endtime = None
      ))
    }

  }

  def main(args: Array[String]) {
    val argsString = args.mkString(",")
    logger.info(s"${argsString}")
    val appid = 1
    createSampleData(appid)

  }
}
