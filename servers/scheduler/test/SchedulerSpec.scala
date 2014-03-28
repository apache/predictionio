package io.prediction.scheduler

import io.prediction.commons.Config
import io.prediction.commons.settings._

import play.api.test._
import play.api.test.Helpers._

import java.io.File

import com.github.nscala_time.time.Imports._
import com.mongodb.casbah.Imports._
import org.apache.commons.io.FileUtils
import org.clapper.scalasti.StringTemplate
import org.specs2._
import org.specs2.matcher.ContentMatchers
import org.specs2.specification.Step

class SchedulerSpec extends Specification with ContentMatchers {
  def is = s2"""
  PredictionIO Scheduler Specification
                                                  ${Step(helloFile.delete())}
                                                  ${Step(FileUtils.touch(helloFile))}
    Synchronize a user                            $userSync
    Setting shared attributes                     $setSharedAttributes
                                                  ${Step(MongoConnection()(config.settingsDbName).dropDatabase())}
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
  val systemInfos = config.getSettingsSystemInfos

  val userid = 1
  val app = App(
    id = 0,
    userid = userid,
    appkey = "appkey",
    display = "",
    url = None,
    cat = None,
    desc = None,
    timezone = "UTC")
  val appid = apps.insert(app)

  val engine = Engine(
    id = 0,
    appid = appid,
    name = "myengine",
    infoid = "itemrec",
    itypes = Some(Seq("movies")),
    params = Map("goal" -> "foobar"))
  val engineid = engines.insert(engine)

  engineInfos.insert(EngineInfo(
    id = "itemrec",
    name = "myengine",
    description = None,
    params = Map(),
    paramsections = Seq(),
    defaultalgoinfoid = "mypkg",
    defaultofflineevalmetricinfoid = "metric",
    defaultofflineevalsplitterinfoid = "splitter"))

  val algo = Algo(
    id = 0,
    engineid = engineid,
    name = "myalgo",
    infoid = "mypkg",
    status = "deployed",
    command = "",
    params = Map(
      "viewParam" -> "2",
      "likeParam" -> "5",
      "dislikeParam" -> "1",
      "conversionParam" -> "4",
      "conflictParam" -> "latest"),
    settings = Map(),
    modelset = false,
    createtime = DateTime.now,
    updatetime = DateTime.now,
    offlineevalid = Some(1),
    offlinetuneid = None)
  val algoid = algos.insert(algo)

  algoInfos.insert(AlgoInfo(
    id = "mypkg",
    name = "mypkg",
    description = None,
    batchcommands = Some(Seq("test/echo.sh $appid$ $engineid$ $algoid$ $conflictParam$ " + helloFilename)),
    offlineevalcommands = None,
    params = Map(),
    paramsections = Seq(),
    paramorder = Seq(),
    engineinfoid = "itemrec",
    techreq = Seq(),
    datareq = Seq()))

  systemInfos.insert(SystemInfo(
    id = "jars.my_custom_algo",
    value = "my.jar",
    description = None))

  systemInfos.insert(SystemInfo(
    id = "jars.foobar",
    value = "foobar.jar",
    description = None))

  def userSync = {
    running(TestServer(5555), HTMLUNIT) { browser =>
      browser.goTo(s"http://localhost:5555/users/${userid}/sync")
      browser.goTo(s"http://localhost:5555/apps/${appid}/engines/${engineid}/trainoncenow")
      helloFile.deleteOnExit()
      helloFile must haveSameLinesAs(Seq(
        Seq(appid, engineid, algoid, "latest") mkString " "
      )).eventually(60, new org.specs2.time.Duration(1000))
    }
  }

  def setSharedAttributes = {
    val template = new StringTemplate("hadoop jar $mahout_core_job$ and $mahout_itemrec$ and $base$/$my_custom_algo$ plus $foobar$")
    val result = Jobs.setSharedAttributes(
      template,
      config,
      app,
      engine,
      Some(algo),
      None,
      None,
      Some(Map(
        "modelset" -> !algo.modelset))).toString

    result must beEqualTo("hadoop jar ../../vendors/mahout-distribution-0.8/mahout-core-0.8-job.jar and ../../lib/predictionio-process-itemrec-algorithms-scala-mahout-assembly-0.7.0-SNAPSHOT.jar and ../../my.jar plus foobar.jar")
  }
}
