package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class OfflineEvalSplitterInfosSpec extends Specification {
  def is =
    "PredictionIO OfflineEvalSplitterInfos Specification" ^
      p ^
      "OfflineEvalSplitterInfos can be implemented by:" ^ endp ^
      "1. MongoOfflineEvalSplitterInfos" ^ mongoOfflineEvalSplitterInfos ^ end

  def mongoOfflineEvalSplitterInfos = p ^
    "MongoOfflineEvalSplitterInfos should" ^
    "behave like any OfflineEvalSplitterInfos implementation" ^ offlineEvalSplitterInfos(newMongoOfflineEvalSplitterInfos) ^
    Step(MongoConnection()(mongoDbName).dropDatabase())

  def offlineEvalSplitterInfos(offlineEvalSplitterInfos: OfflineEvalSplitterInfos) = {
    t ^
      "create and get an splitter info" ! insertAndGet(offlineEvalSplitterInfos) ^
      "get splitter info by engine info id" ! getByEngineinfoid(offlineEvalSplitterInfos) ^
      "update an splitter info" ! update(offlineEvalSplitterInfos) ^
      "delete an splitter info" ! delete(offlineEvalSplitterInfos) ^
      "backup and restore splitter info" ! backuprestore(offlineEvalSplitterInfos) ^
      bt
  }

  val mongoDbName = "predictionio_mongoofflineevalsplitterinfos_test"
  def newMongoOfflineEvalSplitterInfos = new mongodb.MongoOfflineEvalSplitterInfos(MongoConnection()(mongoDbName))

  def insertAndGet(offlineEvalSplitterInfos: OfflineEvalSplitterInfos) = {
    val mapk = OfflineEvalSplitterInfo(
      id = "map-k",
      name = "Mean Average Precision",
      description = None,
      engineinfoids = Seq("itemrec"),
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      params = Map(
        "k" -> Param(
          id = "k",
          name = "k parameter",
          description = Some("Averaging window size"),
          defaultvalue = 20,
          constraint = ParamIntegerConstraint(min = Some(0), max = Some(100)),
          ui = ParamUI(
            uitype = "text"))),
      paramsections = Seq(
        ParamSection(
          name = "foo",
          params = Some(Seq("k")))),
      paramorder = Seq("k"))
    offlineEvalSplitterInfos.insert(mapk)
    offlineEvalSplitterInfos.get("map-k") must beSome(mapk)
  }

  def getByEngineinfoid(offlineEvalSplitterInfos: OfflineEvalSplitterInfos) = {
    val mapkA = OfflineEvalSplitterInfo(
      id = "map-k-a",
      name = "Mean Average Precision A",
      description = None,
      engineinfoids = Seq("engine1"),
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      params = Map(
        "k" -> Param(
          id = "k",
          name = "k parameter",
          description = Some("Averaging window size"),
          defaultvalue = 20,
          constraint = ParamIntegerConstraint(min = Some(0), max = Some(100)),
          ui = ParamUI(
            uitype = "text"))),
      paramsections = Seq(
        ParamSection(
          name = "foo",
          params = Some(Seq("k")))),
      paramorder = Seq("k"))

    val mapkB = mapkA.copy(
      id = "map-k-b",
      name = "Mean Average Precision B",
      engineinfoids = Seq("engine1")
    )

    val mapkC = mapkA.copy(
      id = "map-k-c",
      name = "Mean Average Precision C",
      engineinfoids = Seq("engine2")
    )

    val mapkD = mapkA.copy(
      id = "map-k-D",
      name = "Mean Average Precision D",
      engineinfoids = Seq("engine3", "engine1")
    )

    offlineEvalSplitterInfos.insert(mapkA)
    offlineEvalSplitterInfos.insert(mapkB)
    offlineEvalSplitterInfos.insert(mapkC)
    offlineEvalSplitterInfos.insert(mapkD)

    val engine1Splitters = offlineEvalSplitterInfos.getByEngineinfoid("engine1")

    val engine1Splitter1 = engine1Splitters(0)
    val engine1Splitter2 = engine1Splitters(1)
    val engine1Splitter3 = engine1Splitters(2)

    val engine2Splitters = offlineEvalSplitterInfos.getByEngineinfoid("engine2")

    val engine2Splitter1 = engine2Splitters(0)

    val engine3Splitters = offlineEvalSplitterInfos.getByEngineinfoid("engine3")

    val engine3Splitter1 = engine3Splitters(0)

    engine1Splitters.length must be equalTo (3) and
      (engine1Splitter1 must be equalTo (mapkA)) and
      (engine1Splitter2 must be equalTo (mapkB)) and
      (engine1Splitter3 must be equalTo (mapkD)) and
      (engine2Splitters.length must be equalTo (1)) and
      (engine2Splitter1 must be equalTo (mapkC)) and
      (engine3Splitters.length must be equalTo (1)) and
      (engine3Splitter1 must be equalTo (mapkD))

  }

  def update(offlineEvalSplitterInfos: OfflineEvalSplitterInfos) = {
    val mapk = OfflineEvalSplitterInfo(
      id = "u-map-k",
      name = "Mean Average Precision",
      description = None,
      engineinfoids = Seq("itemrec"),
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      params = Map(
        "k" -> Param(
          id = "k",
          name = "k parameter",
          description = Some("Averaging window size"),
          defaultvalue = 20,
          constraint = ParamIntegerConstraint(min = Some(0), max = Some(100)),
          ui = ParamUI(
            uitype = "text"))),
      paramsections = Seq(
        ParamSection(
          name = "foo",
          params = Some(Seq("k")))),
      paramorder = Seq("k"))
    offlineEvalSplitterInfos.insert(mapk)
    val updatedMapk = mapk.copy(
      commands = Some(Seq(
        "cmd1",
        "cmd2",
        "cmd3")),
      params = Map(
        "k" -> Param(
          id = "k",
          name = "k parameter",
          description = Some("Averaging window size"),
          defaultvalue = 20,
          constraint = ParamIntegerConstraint(min = Some(0), max = Some(100)),
          ui = ParamUI(
            uitype = "text")),
        "f" -> Param(
          id = "f",
          name = "f parameter",
          description = Some("FooBar"),
          defaultvalue = 33,
          constraint = ParamIntegerConstraint(min = Some(1), max = Some(2)),
          ui = ParamUI(
            uitype = "text"))),
      paramsections = Seq(
        ParamSection(
          name = "apple section",
          params = Some(Seq("f", "k")))),
      paramorder = Seq("f", "k"))

    offlineEvalSplitterInfos.update(updatedMapk)
    offlineEvalSplitterInfos.get("u-map-k") must beSome(updatedMapk)
  }

  def delete(offlineEvalSplitterInfos: OfflineEvalSplitterInfos) = {
    val mapk = OfflineEvalSplitterInfo(
      id = "foo",
      name = "Mean Average Precision",
      description = None,
      engineinfoids = Seq("itemrec"),
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      params = Map("k" -> Param(
        id = "k",
        name = "k parameter",
        description = Some("Averaging window size"),
        defaultvalue = 20,
        constraint = ParamIntegerConstraint(min = Some(0), max = Some(100)),
        ui = ParamUI(
          uitype = "text"))),
      paramsections = Seq(
        ParamSection(
          name = "foo",
          params = Some(Seq("k")))),
      paramorder = Seq("k"))
    offlineEvalSplitterInfos.insert(mapk)
    offlineEvalSplitterInfos.delete("foo")
    offlineEvalSplitterInfos.get("foo") must beNone
  }

  def backuprestore(offlineEvalSplitterInfos: OfflineEvalSplitterInfos) = {
    val mapkbk = OfflineEvalSplitterInfo(
      id = "backup",
      name = "Mean Average Precision",
      description = None,
      engineinfoids = Seq("itemrec"),
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      params = Map("k" -> Param(
        id = "k",
        name = "k parameter",
        description = Some("Averaging window size"),
        defaultvalue = 20,
        constraint = ParamIntegerConstraint(min = Some(0), max = Some(100)),
        ui = ParamUI(
          uitype = "text"))),
      paramsections = Seq(
        ParamSection(
          name = "foo",
          params = Some(Seq("k")))),
      paramorder = Seq("k"))
    offlineEvalSplitterInfos.insert(mapkbk)
    val fn = "splitterinfos.json"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(offlineEvalSplitterInfos.backup())
    } finally {
      fos.close()
    }
    offlineEvalSplitterInfos.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.UTF8).mkString.getBytes("UTF-8")) map { data =>
      data must contain(mapkbk)
    } getOrElse 1 === 2
  }
}
