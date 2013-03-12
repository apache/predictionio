package io.prediction.scheduler

import io.prediction.commons.settings._

import play.api.test._
import play.api.test.Helpers._

import java.io.File

import com.mongodb.casbah.Imports._
import org.apache.commons.io.FileUtils
import org.scala_tools.time.Imports.DateTime
import org.specs2._
import org.specs2.specification.Step

class SchedulerSpec extends Specification { def is =
  "PredictionIO Scheduler Specification"                                      ^
                                                                              Step(helloFile.delete()) ^
                                                                              Step(FileUtils.touch(helloFile)) ^
    "Synchronize a user"                                                      ! userSync() ^
                                                                              Step(helloFile.delete()) ^
                                                                              Step(MongoConnection()(config.settingsDbName).dropDatabase()) ^
                                                                              end

  lazy val helloFilename = "test/hello.txt"
  lazy val helloFile = new File(helloFilename)

  /** Setup test data. */
  val config = new Config
  val apps = config.getApps
  val engines = config.getEngines
  val algos = config.getAlgos

  val userid = 1
  val appid = apps.insert(App(
    id = 0,
    userid = userid,
    appkey = "appkey",
    display = "",
    url = None,
    cat = None,
    desc = None,
    timezone = "UTC"
  ))

  val engineid = engines.insert(Engine(
    id = 0,
    appid = appid,
    name = "",
    enginetype = "",
    itypes = Some(List("movies")),
    settings = Map()
  ))

  val algoid = algos.insert(Algo(
    id = 0,
    engineid = engineid,
    name = "myalgo",
    pkgname = "mypkg",
    deployed = true,
    command = "echo \"$appid$ $engineid$ $algoid$ $conflictParam$\" > "+helloFilename,
    params = Map(
      "viewParam"       -> "2",
      "likeParam"       -> "5",
      "dislikeParam"    -> "1",
      "conversionParam" -> "4",
      "conflictParam"   -> "latest"
    ),
    settings = Map(),
    modelset = false,
    createtime = DateTime.now,
    updatetime = DateTime.now,
    offlineevalid = Some(1)
  ))

  def userSync() = {
    running(TestServer(5555), HTMLUNIT) { browser =>
      browser.goTo("http://localhost:5555/users/" + userid + "/sync")
      helloFile.deleteOnExit()
      helloFile must haveSameLinesAs(Seq(
        Seq(appid, engineid, algoid, "latest") mkString " "
      )).eventually(60, 1000.millis)
    }
  }
}
