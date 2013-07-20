package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class OfflineEvalMetricInfosSpec extends Specification { def is =
  "PredictionIO OfflineEvalMetricInfos Specification"                         ^
                                                                              p^
  "OfflineEvalMetricInfos can be implemented by:"                             ^ endp^
    "1. MongoOfflineEvalMetricInfos"                                          ^ mongoOfflineEvalMetricInfos^end

  def mongoOfflineEvalMetricInfos =                                           p^
    "MongoOfflineEvalMetricInfos should"                                      ^
      "behave like any OfflineEvalMetricInfos implementation"                 ^ metricInfos(newMongoOfflineEvalMetricInfos)^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def metricInfos(metricInfos: OfflineEvalMetricInfos) = {                    t^
    "create and get an metric info"                                           ! insertAndGet(metricInfos)^
    "update an metric info"                                                   ! update(metricInfos)^
    "delete an metric info"                                                   ! delete(metricInfos)^
    "backup and restore metric infos"                                         ! backuprestore(metricInfos)^
                                                                              bt
  }

  val mongoDbName = "predictionio_mongometricinfos_test"
  def newMongoOfflineEvalMetricInfos = new mongodb.MongoOfflineEvalMetricInfos(MongoConnection()(mongoDbName))

  def insertAndGet(metricInfos: OfflineEvalMetricInfos) = {
    val mapk = OfflineEvalMetricInfo(
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
    metricInfos.insert(mapk)
    metricInfos.get("map-k") must beSome(mapk)
  }

  def update(metricInfos: OfflineEvalMetricInfos) = {
    val mapk = OfflineEvalMetricInfo(
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
    metricInfos.insert(mapk)
    val updatedMapk = mapk.copy(
      paramdefaults = mapk.paramdefaults ++ Map("f" -> 20),
      paramnames = mapk.paramnames ++ Map("f" -> "Foo"),
      paramdescription = mapk.paramdescription ++ Map("f" -> "FooBar"),
      paramorder = Seq("f", "k"))
    metricInfos.update(updatedMapk)
    metricInfos.get("u-map-k") must beSome(updatedMapk)
  }

  def delete(metricInfos: OfflineEvalMetricInfos) = {
    val mapk = OfflineEvalMetricInfo(
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
    metricInfos.insert(mapk)
    metricInfos.delete("foo")
    metricInfos.get("foo") must beNone
  }

  def backuprestore(metricInfos: OfflineEvalMetricInfos) = {
    val mapkbk = OfflineEvalMetricInfo(
      id = "backup",
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
    metricInfos.insert(mapkbk)
    val fn = "metricinfos.bin"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(metricInfos.backup())
    } finally {
      fos.close()
    }
    metricInfos.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.ISO8859).map(_.toByte).toArray) map { data =>
      data must contain(mapkbk)
    } getOrElse 1 === 2
  }
}
