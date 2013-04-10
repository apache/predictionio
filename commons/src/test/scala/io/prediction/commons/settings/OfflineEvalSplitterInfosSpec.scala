package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class OfflineEvalSplitterInfosSpec extends Specification { def is =
  "PredictionIO OfflineEvalSplitterInfos Specification"                                    ^
                                                                              p^
  "OfflineEvalSplitterInfos can be implemented by:"                                        ^ endp^
    "1. MongoOfflineEvalSplitterInfos"                                                     ^ mongoOfflineEvalSplitterInfos^end

  def mongoOfflineEvalSplitterInfos =                                                      p^
    "MongoOfflineEvalSplitterInfos should"                                                 ^
      "behave like any OfflineEvalSplitterInfos implementation"                            ^ offlineEvalSplitterInfos(newMongoOfflineEvalSplitterInfos)^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def offlineEvalSplitterInfos(offlineEvalSplitterInfos: OfflineEvalSplitterInfos) = {                               t^
    "create and get an metric info"                                           ! insertAndGet(offlineEvalSplitterInfos)^
    "update an metric info"                                                   ! update(offlineEvalSplitterInfos)^
    "delete an metric info"                                                   ! delete(offlineEvalSplitterInfos)^
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
      paramdefaults = Map("k" -> 20),
      paramnames = Map("k" -> "k"),
      paramdescription = Map("k" -> "Averaging window size"),
      paramorder = Seq("k"))
    offlineEvalSplitterInfos.insert(mapk)
    offlineEvalSplitterInfos.get("map-k") must beSome(mapk)
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
      paramdefaults = Map("k" -> 20),
      paramnames = Map("k" -> "k"),
      paramdescription = Map("k" -> "Averaging window size"),
      paramorder = Seq("k"))
    offlineEvalSplitterInfos.insert(mapk)
    val updatedMapk = mapk.copy(
      paramdefaults = mapk.paramdefaults ++ Map("f" -> 20),
      paramnames = mapk.paramnames ++ Map("f" -> "Foo"),
      paramdescription = mapk.paramdescription ++ Map("f" -> "FooBar"),
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
      paramdefaults = Map("k" -> 20),
      paramnames = Map("k" -> "k"),
      paramdescription = Map("k" -> "Averaging window size"),
      paramorder = Seq("k"))
    offlineEvalSplitterInfos.insert(mapk)
    offlineEvalSplitterInfos.delete("foo")
    offlineEvalSplitterInfos.get("foo") must beNone
  }
}
