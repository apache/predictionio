package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class ParamGenInfosSpec extends Specification { def is =
  "PredictionIO ParamGenInfos Specification"                                  ^
                                                                              p^
  "ParamGenInfos can be implemented by:"                                      ^ endp^
    "1. MongoParamGenInfos"                                                   ^ mongoParamGenInfos^end

  def mongoParamGenInfos =                                                    p^
    "MongoParamGenInfos should"                                               ^
      "behave like any ParamGenInfos implementation"                          ^ paramGenInfos(newMongoParamGenInfos)^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def paramGenInfos(paramGenInfos: ParamGenInfos) = {                         t^
    "create and get a parameter generator info"                               ! insertAndGet(paramGenInfos)^
    "update a parameter generator info"                                       ! update(paramGenInfos)^
    "delete a parameter generator info"                                       ! delete(paramGenInfos)^
                                                                              bt
  }

  val mongoDbName = "predictionio_mongoparamgeninfos_test"
  def newMongoParamGenInfos = new mongodb.MongoParamGenInfos(MongoConnection()(mongoDbName))

  def insertAndGet(paramGenInfos: ParamGenInfos) = {
    val mapk = ParamGenInfo(
      id = "map-k",
      name = "Mean Average Precision",
      description = None,
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      paramdefaults = Map("k" -> 20),
      paramnames = Map("k" -> "k"),
      paramdescription = Map("k" -> "Averaging window size"),
      paramorder = Seq("k"))
    paramGenInfos.insert(mapk)
    paramGenInfos.get("map-k") must beSome(mapk)
  }

  def update(paramGenInfos: ParamGenInfos) = {
    val mapk = ParamGenInfo(
      id = "u-map-k",
      name = "Mean Average Precision",
      description = None,
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      paramdefaults = Map("k" -> 20),
      paramnames = Map("k" -> "k"),
      paramdescription = Map("k" -> "Averaging window size"),
      paramorder = Seq("k"))
    paramGenInfos.insert(mapk)
    val updatedMapk = mapk.copy(
      paramdefaults = mapk.paramdefaults ++ Map("f" -> 20),
      paramnames = mapk.paramnames ++ Map("f" -> "Foo"),
      paramdescription = mapk.paramdescription ++ Map("f" -> "FooBar"),
      paramorder = Seq("f", "k"))
    paramGenInfos.update(updatedMapk)
    paramGenInfos.get("u-map-k") must beSome(updatedMapk)
  }

  def delete(paramGenInfos: ParamGenInfos) = {
    val mapk = ParamGenInfo(
      id = "foo",
      name = "Mean Average Precision",
      description = None,
      commands = Some(Seq(
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtKDataPreparator --hdfs --test_dbType $appdataTestDbType$ --test_dbName $appdataTestDbName$ --test_dbHost $appdataTestDbHost$ --test_dbPort $appdataTestDbPort$ --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$ --goalParam $goalParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -Devalid=$evalid$ -Dalgoid=$algoid$ -Dk=$kParam$ -Dmetricid=$metricid$ -Dhdfsroot=$hdfsRoot$ -jar $topkJar$",
        "$hadoop$ jar $pdioEvalJar$ io.prediction.metrics.scalding.itemrec.map.MAPAtK --hdfs --dbType $settingsDbType$ --dbName $settingsDbName$ --dbHost $settingsDbHost$ --dbPort $settingsDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --evalid $evalid$ --metricid $metricid$ --algoid $algoid$ --kParam $kParam$")),
      paramdefaults = Map("k" -> 20),
      paramnames = Map("k" -> "k"),
      paramdescription = Map("k" -> "Averaging window size"),
      paramorder = Seq("k"))
    paramGenInfos.insert(mapk)
    paramGenInfos.delete("foo")
    paramGenInfos.get("foo") must beNone
  }
}
