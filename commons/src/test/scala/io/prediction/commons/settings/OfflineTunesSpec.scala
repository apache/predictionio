package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class OfflineTunesSpec extends Specification { def is =
  "PredictionIO OfflineTunes Specification"               ^
                                                          p^
  "OfflineTunes can be implemented by:"                   ^ endp^
    "1. MongoOfflineTunes"                                ^ mongoOfflineTunes^end

  def mongoOfflineTunes =                                 p^
    "MongoOfflineTunes should"                            ^
      "behave like any OfflinTunes implementation"        ^ offlineTunesTest(newMongoOfflineTunes)^
                                                          Step(MongoConnection()(mongoDbName).dropDatabase())

  def offlineTunesTest(offlineTunes: OfflineTunes) = {    t^
    "create an OfflineTune"                               ! insert(offlineTunes)^
    "update an OfflineTune"                               ! update(offlineTunes)^
    "delete an OfflineTune"                               ! delete(offlineTunes)^
                                                          bt
  }

  val mongoDbName = "predictionio_mongoofflinetunes_test"
  def newMongoOfflineTunes = new mongodb.MongoOfflineTunes(MongoConnection()(mongoDbName))

  /**
   * insert and get by id
   */
  def insert(offlineTunes: OfflineTunes) = {
    val tune1 = OfflineTune(
      id = -1,
      engineid = 4,
      loops = 1,
      createtime = Some(DateTime.now),
      starttime = Some(DateTime.now),
      endtime = None
    )

    val insertid = offlineTunes.insert(tune1)
    offlineTunes.get(insertid) must beSome(tune1.copy(id = insertid))
  }

  /**
   * insert one and then update with new data and get back
   */
  def update(offlineTunes: OfflineTunes) = {
    val tune1 = OfflineTune(
      id = -1,
      engineid = 9,
      loops = 2,
      createtime = None,
      starttime = Some(DateTime.now.hour(3).minute(15).second(8)),
      endtime = None
    )

    val updateid = offlineTunes.insert(tune1)
    val data1 = offlineTunes.get(updateid)

    val tune2 = tune1.copy(
      id = updateid,
      engineid = 10,
      loops = 10,
      createtime = Some(DateTime.now.hour(1).minute(2).second(3)),
      starttime = None,
      endtime = Some(DateTime.now)
    )
    offlineTunes.update(tune2)

    val data2 = offlineTunes.get(updateid)

    data1 must beSome(tune1.copy(id = updateid)) and
      (data2 must beSome(tune2))
  }

  /**
   * insert one and delete and get back
   */
  def delete(offlineTunes: OfflineTunes) = {
    val tune1 = OfflineTune(
      id = -1,
      engineid = 18,
      loops = 11,
      createtime = Some(DateTime.now),
      starttime = Some(DateTime.now),
      endtime = None
    )

    val id1 = offlineTunes.insert(tune1)
    val data1 = offlineTunes.get(id1)

    offlineTunes.delete(id1)
    val data2 = offlineTunes.get(id1)

    data1 must beSome(tune1.copy(id = id1)) and
      (data2 must beNone)
  }
}
