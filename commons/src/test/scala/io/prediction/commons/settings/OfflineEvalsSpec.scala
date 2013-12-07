package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class OfflineEvalsSpec extends Specification {
  def is =
    "PredictionIO OfflineEvals Specification" ^
      p ^
      "OfflineEvals can be implemented by:" ^ endp ^
      "1. MongoOfflineEvals" ^ mongoOfflineEvals ^ end

  def mongoOfflineEvals = p ^
    "MongoOfflineEvals should" ^
    "behave like any OfflineEvals implementation" ^ offlineEvalsTest(newMongoOfflineEvals) ^
    Step(MongoConnection()(mongoDbName).dropDatabase())

  def offlineEvalsTest(offlineEvals: OfflineEvals) = {
    t ^
      "create an OfflineEval" ! insert(offlineEvals) ^
      "get two OfflineEvals by Engineid" ! getByEngineid(offlineEvals) ^
      "get two OfflineEvals by Tuneid" ! getByTuneid(offlineEvals) ^
      "get by id and engineid" ! getByIdAndEngineid(offlineEvals) ^
      "update an OfflineEval" ! update(offlineEvals) ^
      "delete an OfflineEval" ! delete(offlineEvals) ^
      "backup and restore OfflineEvals" ! backuprestore(offlineEvals) ^
      bt
  }

  val mongoDbName = "predictionio_mongoofflineevals_test"
  def newMongoOfflineEvals = new mongodb.MongoOfflineEvals(MongoConnection()(mongoDbName))

  /**
   * insert and get by id
   */
  def insert(offlineEvals: OfflineEvals) = {
    val eval1 = OfflineEval(
      id = -1,
      engineid = 4,
      name = "offline-eval-insert1",
      tuneid = None,
      createtime = Some(DateTime.now),
      starttime = Some(DateTime.now),
      endtime = None
    )

    val insertid = offlineEvals.insert(eval1)
    offlineEvals.get(insertid) must beSome(eval1.copy(id = insertid))
  }

  /**
   * insert a few and get by engineid
   */
  def getByEngineid(offlineEvals: OfflineEvals) = {
    val eval1 = OfflineEval(
      id = -1,
      engineid = 11,
      name = "offline-eval-getByEngineid1",
      tuneid = Some(3),
      createtime = None,
      starttime = None,
      endtime = None
    )
    val eval2 = OfflineEval(
      id = -1,
      engineid = 11,
      name = "offline-eval-getByEngineid2",
      tuneid = None,
      createtime = Some(DateTime.now.hour(1).minute(12).second(34)),
      starttime = Some(DateTime.now.hour(2).minute(45).second(10)),
      endtime = Some(DateTime.now.hour(4).minute(56).second(35))
    )

    val id1 = offlineEvals.insert(eval1)
    val id2 = offlineEvals.insert(eval2)

    val it = offlineEvals.getByEngineid(11)

    val it1 = it.next()
    val it2 = it.next()
    val left = it.hasNext // make sure it has 2 only

    it1 must be equalTo (eval1.copy(id = id1)) and
      (it2 must be equalTo (eval2.copy(id = id2))) and
      (left must be_==(false))

  }

  /**
   * insert a few and get by offline tune id
   */
  def getByTuneid(offlineEvals: OfflineEvals) = {
    val eval1 = OfflineEval(
      id = -1,
      engineid = 12,
      name = "offline-eval-getByEngineid1",
      tuneid = Some(31),
      createtime = None,
      starttime = None,
      endtime = None
    )
    val eval2 = OfflineEval(
      id = -1,
      engineid = 12,
      name = "offline-eval-getByEngineid2",
      tuneid = Some(31),
      createtime = Some(DateTime.now.hour(1).minute(12).second(34)),
      starttime = Some(DateTime.now.hour(2).minute(45).second(10)),
      endtime = Some(DateTime.now.hour(4).minute(56).second(35))
    )
    val eval3 = OfflineEval(
      id = -1,
      engineid = 12,
      name = "offline-eval-getByEngineid2",
      tuneid = Some(32), // note: this one has different tuneid
      createtime = Some(DateTime.now.hour(1).minute(12).second(34)),
      starttime = Some(DateTime.now.hour(2).minute(45).second(10)),
      endtime = Some(DateTime.now.hour(4).minute(56).second(35))
    )

    val id1 = offlineEvals.insert(eval1)
    val id2 = offlineEvals.insert(eval2)
    val id3 = offlineEvals.insert(eval3)

    val it = offlineEvals.getByTuneid(31)

    val it1 = it.next()
    val it2 = it.next()
    val left = it.hasNext // make sure it has 2 only

    it1 must be equalTo (eval1.copy(id = id1)) and
      (it2 must be equalTo (eval2.copy(id = id2))) and
      (left must be_==(false))

  }

  def getByIdAndEngineid(offlineEvals: OfflineEvals) = {
    val obj1 = OfflineEval(
      id = -1,
      engineid = 2345,
      name = "getByIdAndEngineid",
      tuneid = Some(3),
      createtime = None,
      starttime = None,
      endtime = None
    )
    val obj2 = obj1.copy()
    val obj3 = obj1.copy(engineid = 2346, name = "getByIdAndEngineid3")

    val id1 = offlineEvals.insert(obj1)
    val id2 = offlineEvals.insert(obj2)
    val id3 = offlineEvals.insert(obj3)
    val eval1 = offlineEvals.getByIdAndEngineid(id1, 2345)
    val eval1b = offlineEvals.getByIdAndEngineid(id1, 2346)
    val eval2 = offlineEvals.getByIdAndEngineid(id2, 2345)
    val eval2b = offlineEvals.getByIdAndEngineid(id2, 2346)
    val eval3b = offlineEvals.getByIdAndEngineid(id3, 2345)
    val eval3 = offlineEvals.getByIdAndEngineid(id3, 2346)

    eval1 must beSome(obj1.copy(id = id1)) and
      (eval1b must beNone) and
      (eval2 must beSome(obj2.copy(id = id2))) and
      (eval2b must beNone) and
      (eval3 must beSome(obj3.copy(id = id3))) and
      (eval3b must beNone)
  }

  /**
   * insert one and then update with new data and get back
   */
  def update(offlineEvals: OfflineEvals) = {
    val eval1 = OfflineEval(
      id = -1,
      engineid = 9,
      name = "offline-eval-update1",
      tuneid = None,
      createtime = None,
      starttime = Some(DateTime.now.hour(3).minute(15).second(8)),
      endtime = None
    )

    val updateid = offlineEvals.insert(eval1)
    val data1 = offlineEvals.get(updateid)

    val eval2 = eval1.copy(
      id = updateid,
      engineid = 10,
      name = "new-offline-eval-update1",
      tuneid = Some(44),
      createtime = Some(DateTime.now.hour(1).minute(2).second(3)),
      starttime = None,
      endtime = Some(DateTime.now)
    )
    offlineEvals.update(eval2)

    val data2 = offlineEvals.get(updateid)

    data1 must beSome(eval1.copy(id = updateid)) and
      (data2 must beSome(eval2))
  }

  /**
   * insert one and delete and get back
   */
  def delete(offlineEvals: OfflineEvals) = {
    val eval1 = OfflineEval(
      id = -1,
      engineid = 18,
      name = "offline-eval-delete",
      tuneid = None,
      createtime = Some(DateTime.now),
      starttime = Some(DateTime.now),
      endtime = None
    )

    val id1 = offlineEvals.insert(eval1)
    val data1 = offlineEvals.get(id1)

    offlineEvals.delete(id1)
    val data2 = offlineEvals.get(id1)

    data1 must beSome(eval1.copy(id = id1)) and
      (data2 must beNone)
  }

  def backuprestore(offlineEvals: OfflineEvals) = {
    val eval1 = OfflineEval(
      id = -1,
      engineid = 20,
      name = "backuprestore",
      tuneid = None,
      createtime = Some(DateTime.now),
      starttime = Some(DateTime.now),
      endtime = None
    )
    val id1 = offlineEvals.insert(eval1)
    val fn = "evals.bin"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(offlineEvals.backup())
    } finally {
      fos.close()
    }
    offlineEvals.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.ISO8859).map(_.toByte).toArray) map { data =>
      // For some reason inserting Joda DateTime to DB and getting them back will make test pass
      val feval1 = data.find(_.id == id1).get
      offlineEvals.update(feval1)
      offlineEvals.get(id1) must beSome(eval1.copy(id = id1))
    } getOrElse 1 === 2
  }
}
