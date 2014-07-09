package io.prediction.engines.itemrank

import io.prediction.storage.Storage
import io.prediction.storage.{ User, Item, U2IAction, ItemSet }

import scala.util.Random
import grizzled.slf4j.Logger
import com.github.nscala_time.time.Imports._

object CreateSampleData {

  val logger = Logger(CreateSampleData.getClass)
  val rand = new Random(0) // random with seed

  val usersDb = Storage.getAppdataUsers
  val itemsDb = Storage.getAppdataItems
  val u2iDb = Storage.getAppdataU2IActions
  val itemSetsDb = Storage.getAppdataItemSets

  def createSampleData(appid: Int, days: Int) = {

    // remove old data if exists
    usersDb.deleteByAppid(appid)
    itemsDb.deleteByAppid(appid)
    u2iDb.deleteByAppid(appid)
    itemSetsDb.deleteByAppid(appid)

    val userIds = Range(0, 10).map(i => s"u${i}").toList
    val itemIds = Range(0, 100).map(i => s"i${i}").toList

    val refTime = new DateTime("2014-04-01T06:10:51.754-07:00")
    val itemSetIds = Range(0, days).toList.map { d =>
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

  case class Args(
    appid: Int = 1,
    days: Int = 30
  )

  def main(args: Array[String]) {
    val argsString = args.mkString(",")
    logger.info(s"${argsString}")
    val appid = 1
    val parser = new scopt.OptionParser[Args]("CreateSampleData") {
        head("CreateSampleData", "0.x")
        help("help") text ("prints this usage text")
        opt[Int]("appid").optional()
          .valueName("<app id>").action { (x, c) =>
            c.copy(appid = x) }
        opt[Int]("days").optional()
          .valueName("<number of days>").action { (x, c) =>
            c.copy(days = x) }
    }

    val arg: Option[Args] = parser.parse(args, Args())

    if (arg == None) {
      error("Invalid arguments")
      System.exit(1)
    }

    arg.map { a =>
      createSampleData(a.appid, a.days)
    }

  }
}
