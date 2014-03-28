package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class AppsSpec extends Specification {
  def is = s2"""

  PredictionIO Apps Specification

    Apps can be implemented by:
    - MongoApps ${mongoApps}

  """

  def mongoApps = s2"""

    MongoApps should
    - behave like any Apps implementation ${apps(newMongoApps)}
    - (database cleanup) ${Step(MongoConnection()(mongoDbName).dropDatabase())}

  """

  def apps(apps: Apps) = s2"""

    get two apps by user ID ${getByUserid(apps)}
    get an app by its appkey ${getByAppkey(apps)}
    get an app by a non-existing appkey and fail ${getByAppkeyNonExist(apps)}
    get an app by its appkey and user ID ${getByAppkeyAndUserid(apps)}
    get an app by its appkey and a non-existing user ID and fail ${getByAppkeyAndUseridNonExist(apps)}
    get an app by its ID and user ID ${getByIdAndUserid(apps)}
    get an app by a non-existing ID and user ID and fail ${getByIdAndUseridNonExist(apps)}
    delete an app by its ID and user ID ${deleteByIdAndUserid(apps)}
    check existence of an app by its ID, appkey and user ID ${existsByIdAndAppkeyAndUserid(apps)}
    updating an app ${update(apps)}
    updating an app's appkey ${updateAppkeyByAppkeyAndUserid(apps)}
    updating an app's timezone ${updateTimezoneByAppkeyAndUserid(apps)}
    backup and restore apps ${backuprestore(apps)}

  """

  val mongoDbName = "predictionio_mongoapps_test"
  def newMongoApps = new mongodb.MongoApps(MongoConnection()(mongoDbName))

  def dummyApp(id: Int, userid: Int, dummy: String) = App(
    id = id,
    userid = userid,
    appkey = dummy,
    display = dummy,
    url = None,
    cat = None,
    desc = None,
    timezone = "UTC"
  )

  def getByUserid(apps: Apps) = {
    val userid = 47838
    val dummy1 = dummyApp(0, userid, "getByUserid1")
    val dummy2 = dummyApp(0, userid, "getByUserid2")
    val id1 = apps.insert(dummy1)
    val id2 = apps.insert(dummy2)
    val app12 = apps.getByUserid(userid)
    val app1 = app12.next()
    val app2 = app12.next()
    (app1 must be equalTo (dummy1.copy(id = id1))) and
      (app2 must be equalTo (dummy2.copy(id = id2)))
  }

  def getByAppkey(apps: Apps) = {
    val userid = 2345
    val dummy = dummyApp(0, userid, "getByAppkey")
    val id = apps.insert(dummy)
    apps.getByAppkey("getByAppkey") must beSome(dummy.copy(id = id))
  }

  def getByAppkeyNonExist(apps: Apps) = {
    apps.getByAppkey("getByAppkeyNonExist") must beNone
  }

  def getByAppkeyAndUserid(apps: Apps) = {
    val name = "getByAppkeyAndUserid"
    val userid = 689
    val dummy = dummyApp(0, userid, name)
    val id = apps.insert(dummy)
    apps.getByAppkeyAndUserid(name, userid) must beSome(dummy.copy(id = id))
  }

  def getByAppkeyAndUseridNonExist(apps: Apps) = {
    val userid = 203
    val id = apps.insert(dummyApp(0, userid, "getByAppkeyAndUseridNonExist"))
    apps.getByAppkeyAndUserid("getByAppkeyAndUseridNonExist", 2849) must beNone
  }

  def getByIdAndUserid(apps: Apps) = {
    val name = "getByIdAndUserid"
    val userid = 12
    val dummy = dummyApp(0, userid, name)
    val id = apps.insert(dummy)
    apps.getByIdAndUserid(id, userid) must beSome(dummy.copy(id = id))
  }

  def getByIdAndUseridNonExist(apps: Apps) = {
    val name = "getByIdAndUseridNonExist"
    val userid = 23
    val id = apps.insert(dummyApp(0, userid, name))
    apps.getByIdAndUserid(0, userid) must beNone
  }

  def deleteByIdAndUserid(apps: Apps) = {
    val name = "deleteByIdAndUserid"
    val userid = 34
    val id = apps.insert(dummyApp(0, userid, name))
    apps.deleteByIdAndUserid(id, userid)
    apps.getByIdAndUserid(id, userid) must beNone
  }

  def existsByIdAndAppkeyAndUserid(apps: Apps) = {
    val name = "existsByIdAndAppkeyAndUserid"
    val userid = 45
    val id = apps.insert(dummyApp(0, userid, name))
    apps.existsByIdAndAppkeyAndUserid(id, name, userid) must beTrue
  }

  def update(apps: Apps) = {
    val name = "update"
    val userid = 56
    val id = apps.insert(dummyApp(0, userid, name))
    val updated = dummyApp(id, 67, "updated")
    apps.update(updated)
    apps.getByIdAndUserid(id, 67) must beSome(updated)
  }

  def updateAppkeyByAppkeyAndUserid(apps: Apps) = {
    val name = "updateAppkeyByAppkeyAndUserid"
    val userid = 78
    val id = apps.insert(dummyApp(0, userid, name))
    val updated = dummyApp(id, 67, "updated")
    apps.updateAppkeyByAppkeyAndUserid(name, userid, "updatedAppkey")
    apps.existsByIdAndAppkeyAndUserid(id, "updatedAppkey", userid) must beTrue
  }

  def updateTimezoneByAppkeyAndUserid(apps: Apps) = {
    val name = "updateTimezoneByAppkeyAndUserid"
    val userid = 89
    val id = apps.insert(dummyApp(0, userid, name))
    val updated = App(
      id = id,
      userid = userid,
      appkey = name,
      display = name,
      url = None,
      cat = None,
      desc = None,
      timezone = "US/Pacific"
    )
    apps.updateTimezoneByAppkeyAndUserid(name, userid, "US/Pacific")
    apps.getByAppkey(name) must beSome(updated)
  }

  def backuprestore(apps: Apps) = {
    val name = "backuprestore"
    val userid = 90
    val app1 = dummyApp(0, userid, name)
    val id1 = apps.insert(app1)
    val fn = "apps.json"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(apps.backup())
    } finally {
      fos.close()
    }
    apps.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.UTF8).mkString.getBytes("UTF-8")) map { data =>
      data must contain(app1.copy(id = id1))
    } getOrElse 1 === 2
  }
}
