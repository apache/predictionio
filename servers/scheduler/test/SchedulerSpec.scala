package io.prediction.scheduler

import io.prediction.commons.Config
import io.prediction.commons.settings._

import play.api.test._
import play.api.test.Helpers._

import java.io.File

import com.github.nscala_time.time.Imports._
import com.mongodb.casbah.Imports._
import org.apache.commons.io.FileUtils
import org.specs2._
import org.specs2.matcher.ContentMatchers
import org.specs2.specification.Step

class SchedulerSpec extends Specification with ContentMatchers { def is = s2"""
  PredictionIO Scheduler Specification
                                                  ${ Step(helloFile.delete()) }
                                                  ${ Step(FileUtils.touch(helloFile)) }
    Synchronize a user                            $userSync
                                                  ${ Step(MongoConnection()(config.settingsDbName).dropDatabase()) }
  """

  lazy val helloFilename = "test/hello.txt"
  lazy val helloFile = new File(helloFilename)

  /** Setup test data. */
  val config = new Config
  val apps = config.getSettingsApps
  val engines = config.getSettingsEngines
  val engineInfos = config.getSettingsEngineInfos
  val algos = config.getSettingsAlgos
  val algoInfos = config.getSettingsAlgoInfos

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
    name = "myengine",
    infoid = "itemrec",
    itypes = Some(Seq("movies")),
    settings = Map("goal" -> "foobar")
  ))

  engineInfos.insert(EngineInfo(
    id = "itemrec",
    name = "myengine",
    description = None,
    defaultsettings = Map(),
    defaultalgoinfoid = "mypkg"))

  val algoid = algos.insert(Algo(
    id = 0,
    engineid = engineid,
    name = "myalgo",
    infoid = "mypkg",
    status = "deployed",
    command = "",
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
    offlineevalid = Some(1),
    offlinetuneid = None
  ))

  algoInfos.insert(AlgoInfo(
    id = "mypkg",
    name = "mypkg",
    description = None,
    batchcommands = Some(Seq("test/echo.sh $appid$ $engineid$ $algoid$ $conflictParam$ "+helloFilename)),
    offlineevalcommands = None,
    params = Map(),
    paramorder = Seq(),
    engineinfoid = "itemrec",
    techreq = Seq(),
    datareq = Seq()))

  def userSync = {
    running(TestServer(5555), HTMLUNIT) { browser =>
      browser.goTo("http://localhost:5555/users/" + userid + "/sync")
      helloFile.deleteOnExit()
      helloFile must haveSameLinesAs(Seq(
        Seq(appid, engineid, algoid, "latest") mkString " "
      )).eventually(60, new org.specs2.time.Duration(1000))
    }
  }
}
