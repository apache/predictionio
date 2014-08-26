package io.prediction.engines.itemrank

// This module allows users to evaluate their algorithm data with their acutal
// data. It takes the mongodb dump (from Version 0.7 or before) of the three key
// collections: User, Item, and U2IActions. It evaluates the data in a
// time-rolling fashsion. First, it sets a cutoff day d and uses the data before
// it for training, and then use the following n days for testing. Then, it
// shifts the cutoff day by n, i.e. using data before (d + n-days) for training,
// then use data between d + n and d + 2n for testing, and so on until the last
// day.
// 
// In each test, we construct a query using a combination of 1. all conversion
// data of a user on a day; and 2. the global top x sold items of that day.
// 
// Notice that this replay may not be completely accurate as User and Item are
// not event log.

import io.prediction.controller._
import com.github.nscala_time.time.Imports._
import org.joda.time.Instant
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.io.Source
import scala.collection.immutable.HashMap
import io.prediction.workflow.APIDebugWorkflow
import scala.util.hashing.MurmurHash3

case class ReplaySliceParams(val name: String) extends Serializable with HasName

case class ReplayDataSourceParams(
  val userPath: String,
  val itemPath: String,
  val u2iPath: String,
  val baseDate: LocalDate,
  val fromIdx: Int,
  val untilIdx: Int, 
  val testingWindowSize: Int,
  // Mix top x sold items in Query
  val numTopSoldItems: Int,
  // Only items belonging to this whitelist is considered.
  val whitelistItypes: Seq[String],
  // Only u2i belonging to this single action is considered.
  val whitelistAction: String
) extends Params


class ReplayDataSource(val dsp: ReplayDataSourceParams)
  extends LDataSource[
      DataSourceParams, ReplaySliceParams, TrainingData, Query, Actual] {

  def load(): (Array[User], Array[Item], Array[U2I]) = {
    implicit val formats = DefaultFormats
    
    val u2iList = Source
      .fromFile(dsp.u2iPath).getLines
      .map { s => parse(s).extract[U2I] }
      .toArray

    val userList = Source
      .fromFile(dsp.userPath).getLines
      .map { s => parse(s).extract[User] }
      .toArray
    
    val itemList = Source
      .fromFile(dsp.itemPath)
      .getLines
      .map { s => parse(s).extract[Item] }
      .toArray

    return (userList, itemList, u2iList)
  }

  def preprocess(input: (Array[User],  Array[Item], Array[U2I]))
  : (Array[User], Array[Item], Map[LocalDate, Array[U2I]]) = {
    val (users, items, u2is) = input

    val whitelistItypeSet = Set(dsp.whitelistItypes:_*)
    val validItems: Array[Item] = items
      .filter(_.itypes.find(it => whitelistItypeSet(it)) != None)
    val validIidSet: Set[String] = validItems.map(_._id).toSet

    val date2Actions: Map[LocalDate, Array[U2I]] = u2is
      .filter(u2i => validIidSet(u2i.iid))
      .filter(u2i => u2i.action == dsp.whitelistAction)
      .groupBy(_.dt.toLocalDate)

    (users, validItems, date2Actions)
  }

  def generate(input: (Array[User], Array[Item], Map[LocalDate, Array[U2I]]))
    //userList: Array[User], 
    //itemList: Array[Item],
    //date2u2iList: Map[LocalDate, Seq[U2I]])
  : Seq[(ReplaySliceParams, TrainingData, Array[(Query, Actual)])] = {
    //val random = new scala.util.Random(0)
    
    //val (userList: Array[User], 
    //  itemList: Array[Item],
    //  date2u2iList: Map[LocalDate, Seq[U2I]]) = input

    val (userList, itemList, date2u2iList)
    : (Array[User], Array[Item], Map[LocalDate, Array[U2I]]) = input

    val ui2UserTd: Map[Int, UserTD] = userList
      .zipWithIndex
      .map(_.swap)
      .toMap
      .mapValues(user => new UserTD(user._id))
    val ui2uid: Map[Int, String] = ui2UserTd.mapValues(_.uid)
    val uid2ui: Map[String, Int] = ui2uid.map(_.swap)

    val ii2ItemTd: Map[Int, ItemTD] = itemList
      .zipWithIndex
      .map(_.swap)
      .toMap
      .mapValues(
        item => new ItemTD(item._id, item.itypes.toSeq, None, None, false))
    val ii2iid: Map[Int, String] = ii2ItemTd.mapValues(_.iid)
    val iid2ii: Map[String, Int] = ii2iid.map(_.swap)

    val date2Actions: Map[LocalDate, Array[U2IActionTD]] = date2u2iList
      .mapValues(
        _.map(u2i => new U2IActionTD(
          uid2ui(u2i.uid), 
          iid2ii(u2i.iid),
          u2i.action,
          None,
          u2i.t.$date)))
      
    val dailyServedItems: Map[LocalDate, Array[(String, Int)]] = date2u2iList
      .mapValues(
        _.map(_.iid).groupBy(identity).mapValues(_.size).toArray.sortBy(-_._2)
      )
    
    Range(dsp.fromIdx, dsp.untilIdx, dsp.testingWindowSize).map { idx => {
      val trainingUntilDate: LocalDate = dsp.baseDate.plusDays(idx)
      println("TrainingUntil: " + trainingUntilDate.toString)

      val trainingDate2Actions: Map[LocalDate, Array[U2IActionTD]] = 
        date2Actions.filterKeys(k => k.isBefore(trainingUntilDate))

      val trainingActions: Array[U2IActionTD] = trainingDate2Actions
        .values
        .flatMap(_.toSeq)
        .toArray

      val trainingData = new TrainingData(
        HashMap[Int, UserTD]() ++ ui2UserTd,
        HashMap[Int, ItemTD]() ++ ii2ItemTd,
        Array[U2IActionTD]() ++ trainingActions)
      
      val uiActionsMap: Map[Int, Int] = trainingActions
        .groupBy(_.uindex)
        .mapValues(_.size)

      // Seq[(Int, Int)]: (User, Order Size)
      val date2OrderSizeMap: Map[LocalDate, Array[(Int, Int)]] = 
      trainingDate2Actions
        .mapValues { 
          _.groupBy(_.uindex).mapValues(_.size).toArray
        }

      val uiAverageSizeMap: Map[Int, Double] = date2OrderSizeMap
        .values
        //.flatMap(identity)
        .flatMap(_.toSeq)
        .groupBy(_._1)
        .mapValues( l => l.map(_._2).sum.toDouble / l.size )
      
      val uiPreviousOrdersMap: Map[Int, Int] = date2OrderSizeMap
        .values
        //.flatMap(identity)
        .flatMap(_.toSeq)
        .groupBy(_._1)
        .mapValues(_.size)

      val uiVarietyMap: Map[Int, Int] = trainingActions
        .groupBy(_.uindex)
        .mapValues(_.map(_.iindex).distinct.size)

      val queryActionList: Array[(Query, Actual)] =
      Range(idx, math.min(idx + dsp.testingWindowSize, dsp.untilIdx))
      .map { queryIdx => dsp.baseDate.plusDays(queryIdx) }
      .flatMap { queryDate => {
        println(
          s"Testing: ${queryDate.toString} DOW(${queryDate.getDayOfWeek})")
        val u2is = date2u2iList.getOrElse(queryDate, Array[U2I]())
        val uid2Actions: Map[String, Array[U2I]] = u2is.groupBy(_.uid)

        val user2iids = uid2Actions.mapValues(_.map(_.iid))
        // Use first action time.
        val user2LocalDT = uid2Actions
          .mapValues(_.map(_.dt.toLocalDateTime).min)
        
        val todayItems: Seq[String] = dailyServedItems(queryDate)
          .take(dsp.numTopSoldItems)
          .map(_._1)

        user2iids.map { case (uid, iids) => {
          val possibleIids = (iids ++ todayItems)
            .distinct

          //val sortedIids = random.shuffle(possibleIids)
          // Introduce some kind of stable randomness 
          val sortedIids = possibleIids
            .sortBy(iid => MurmurHash3.stringHash(iid))
          //val sortedIids = possibleIids.sortBy(identity)

          //val query = new Query(uid, possibleIids)
          val query = new Query(uid, sortedIids)
          val ui = uid2ui(uid)

          val actual = new Actual(
            items = iids.toSeq,
            previousActionCount = uiActionsMap.getOrElse(ui, 0),
            localDate = queryDate,
            localDateTime = user2LocalDT(uid),
            averageOrderSize = uiAverageSizeMap.getOrElse(ui, 0),
            previousOrders = uiPreviousOrdersMap.getOrElse(ui, 0),
            variety = uiVarietyMap.getOrElse(ui, 0)
          )
          (query, actual)
        }}
      }}
      .toArray

      val trainingUntilDT = trainingUntilDate.toDateTimeAtStartOfDay

      val dow = trainingUntilDate.dayOfWeek.getAsShortText
      val dp = ReplaySliceParams(s"${trainingUntilDate.toString()} $dow")

      println("Testing Size: " + queryActionList.size)
      (dp, trainingData, queryActionList)
    }}

  }

  override
  def read(): Seq[(ReplaySliceParams, TrainingData, Seq[(Query, Actual)])] = {
    val (userList, itemList, date2u2iList)
    : (Array[User], Array[Item], Map[LocalDate, Array[U2I]]) 
    = preprocess(load())

    generate((userList, itemList, date2u2iList))
    .map(e => (e._1, e._2, e._3.toSeq))
  }
}

case class DateObject(val $date: Long)

case class U2I(
  val action: String, 
  val uid: String, val iid: String, val t: DateObject) {
  lazy val dt: DateTime = 
    new DateTime(new Instant(t.$date), DateTimeZone.forOffsetHours(-8))

  override def toString(): String = 
    s"U2I($uid, $iid, $action, $dt)"
}

case class User(val _id: String)

case class Item(val _id: String, val starttime: DateObject,
  val itypes: Array[String],
  val ca_name: String) {
  override def toString(): String = 
    s"${_id} $ca_name [" + itypes.mkString(",") + "]"

  val itypesSet = Set(itypes:_*)
}

