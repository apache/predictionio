package io.prediction.commons.settings

import io.prediction.commons.Spec

import org.specs2._
import org.specs2.specification.Step

import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class OfflineTunesSpec extends Specification {
  def is = s2"""

  PredictionIO OfflineTunes Specification

    OfflineTunes can be implemented by:
    - MongoOfflineTunes ${mongoOfflineTunes}

  """

  def mongoOfflineTunes = s2"""

    MongoOfflineTunes should
    - behave like any OfflinTunes implementation ${offlineTunesTest(newMongoOfflineTunes)}
    - (database cleanup) ${Step(Spec.mongoClient(mongoDbName).dropDatabase())}

  """

  def offlineTunesTest(offlineTunes: OfflineTunes) = s2"""

    create an OfflineTune ${insert(offlineTunes)}
    update an OfflineTune ${update(offlineTunes)}
    delete an OfflineTune ${delete(offlineTunes)}
    backup and restore OfflineTunes ${backuprestore(offlineTunes)}

  """

  val mongoDbName = "predictionio_mongoofflinetunes_test"
  def newMongoOfflineTunes = new mongodb.MongoOfflineTunes(Spec.mongoClient(mongoDbName))

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

  def backuprestore(offlineTunes: OfflineTunes) = {
    val tune1 = OfflineTune(
      id = -1,
      engineid = 18,
      loops = 11,
      createtime = Some(DateTime.now),
      starttime = Some(DateTime.now),
      endtime = None
    )
    val id1 = offlineTunes.insert(tune1)
    val fn = "tunes.json"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(offlineTunes.backup())
    } finally {
      fos.close()
    }
    offlineTunes.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.UTF8).mkString.getBytes("UTF-8")) map { data =>
      // For some reason inserting Joda DateTime to DB and getting them back will make test pass
      val ftune1 = data.find(_.id == id1).get
      offlineTunes.update(ftune1)
      offlineTunes.get(id1) must beSome(tune1.copy(id = id1))
    } getOrElse 1 === 2
  }
}
