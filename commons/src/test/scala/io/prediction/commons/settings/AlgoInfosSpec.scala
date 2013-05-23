package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class AlgoInfosSpec extends Specification { def is =
  "PredictionIO AlgoInfos Specification"                                      ^
                                                                              p ^
  "Algos can be implemented by:"                                              ^ endp ^
    "1. MongoAlgoInfos"                                                       ^ mongoAlgoInfos^end

  def mongoAlgoInfos =                                                        p ^
    "MongoAlgoInfos should"                                                   ^
      "behave like any AlgoInfos implementation"                              ^ algoinfos(newMongoAlgoInfos) ^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def algoinfos(algoinfos: AlgoInfos) = {                                     t ^
    "insert and get info of an algo"                                          ! insertAndGet(algoinfos) ^
    "get info of algos by their engine type"                                  ! getByEngineInfoId(algoinfos) ^
    "update info of an algo"                                                  ! update(algoinfos) ^
    "delete info of an algo"                                                  ! delete(algoinfos) ^
                                                                              bt
  }

  val mongoDbName = "predictionio_mongoalgoinfos_test"
  def newMongoAlgoInfos = new mongodb.MongoAlgoInfos(MongoConnection()(mongoDbName))

  def insertAndGet(algoinfos: AlgoInfos) = {
    algoinfos.insert(AlgoInfo(
      id = "pdio-knnitembased",
      name = "kNN Item Based Collaborative Filtering",
      description = Some("This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false")),
      paramdefaults = Map(
        "measureParam" -> "correl",
        "priorCountParam" -> 20,
        "priorCorrelParam" -> 0,
        "minNumRatersParam" -> 1,
        "maxNumRatersParam" -> 10000,
        "minIntersectionParam" -> 1,
        "minNumRatedSimParam" -> 1,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest",
        "priorCountParamMin" -> 10,
        "priorCountParamMax" -> 30,
        "priorCorrelParamMin" -> 0.0,
        "priorCorrelParamMax" -> 0.1,
        "minNumRatersParamMin" -> 1,
        "minNumRatersParamMax" -> 5,
        "maxNumRatersParamMin" -> 10000,
        "maxNumRatersParamMax" -> 10000,
        "minIntersectionParamMin" -> 1,
        "minIntersectionParamMax" -> 5,
        "minNumRatedSimParamMin" -> 1,
        "minNumRatedSimParamMax" -> 5
        ),
      paramnames = Map(
        "measureParam" -> "Distance Function",
        "priorCountParam" -> "Virtual Count",
        "priorCorrelParam" -> "Prior Correlation",
        "minNumRatersParam" -> "Minimum Number of Raters",
        "maxNumRatersParam" -> "Maximum Number of Raters",
        "minIntersectionParam" -> "Minimum Intersection",
        "minNumRatedSimParam" -> "Minimum Number of Rated Similar Items",
        "viewParam" -> "View Score",
        "viewmoreParam" -> "View More Score",
        "likeParam" -> "Like Score",
        "dislikeParam" -> "Dislike Score",
        "conversionParam" -> "Buy Score",
        "conflictParam" -> "Override"),
      paramdescription = Map(
        "measureParam" -> "",
        "priorCountParam" -> "Suggested range: 0 to 100.",
        "priorCorrelParam" -> "",
        "minNumRatersParam" -> "",
        "maxNumRatersParam" -> "",
        "minIntersectionParam" -> "",
        "minNumRatedSimParam" -> "",
        "viewParam" -> "",
        "viewmoreParam" -> "",
        "likeParam" -> "",
        "dislikeParam" -> "",
        "conversionParam" -> "",
        "conflictParam" -> ""),
      paramorder = Seq(
        "measureParam",
        "priorCountParam",
        "priorCorrelParam",
        "minNumRatersParam",
        "maxNumRatersParam",
        "minIntersectionParam",
        "minNumRatedSimParam",
        "viewParam",
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      engineinfoid = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")))
    algoinfos.get("pdio-knnitembased").get.name must beEqualTo("kNN Item Based Collaborative Filtering")
  }

  def getByEngineInfoId(algoinfos: AlgoInfos) = {
    algoinfos.insert(AlgoInfo(
      id = "pdio-is1",
      name = "kNN Item Based Collaborative Filtering",
      description = Some("This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false")),
      paramdefaults = Map(
        "measureParam" -> "correl",
        "priorCountParam" -> 20,
        "priorCorrelParam" -> 0,
        "minNumRatersParam" -> 1,
        "maxNumRatersParam" -> 10000,
        "minIntersectionParam" -> 1,
        "minNumRatedSimParam" -> 1,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest",
        "priorCountParamMin" -> 10,
        "priorCountParamMax" -> 30,
        "priorCorrelParamMin" -> 0.0,
        "priorCorrelParamMax" -> 0.1,
        "minNumRatersParamMin" -> 1,
        "minNumRatersParamMax" -> 5,
        "maxNumRatersParamMin" -> 10000,
        "maxNumRatersParamMax" -> 10000,
        "minIntersectionParamMin" -> 1,
        "minIntersectionParamMax" -> 5,
        "minNumRatedSimParamMin" -> 1,
        "minNumRatedSimParamMax" -> 5
        ),
      paramnames = Map(
        "measureParam" -> "Distance Function",
        "priorCountParam" -> "Virtual Count",
        "priorCorrelParam" -> "Prior Correlation",
        "minNumRatersParam" -> "Minimum Number of Raters",
        "maxNumRatersParam" -> "Maximum Number of Raters",
        "minIntersectionParam" -> "Minimum Intersection",
        "minNumRatedSimParam" -> "Minimum Number of Rated Similar Items",
        "viewParam" -> "View Score",
        "viewmoreParam" -> "View More Score",
        "likeParam" -> "Like Score",
        "dislikeParam" -> "Dislike Score",
        "conversionParam" -> "Buy Score",
        "conflictParam" -> "Override"),
      paramdescription = Map(
        "measureParam" -> "",
        "priorCountParam" -> "Suggested range: 0 to 100.",
        "priorCorrelParam" -> "",
        "minNumRatersParam" -> "",
        "maxNumRatersParam" -> "",
        "minIntersectionParam" -> "",
        "minNumRatedSimParam" -> "",
        "viewParam" -> "",
        "viewmoreParam" -> "",
        "likeParam" -> "",
        "dislikeParam" -> "",
        "conversionParam" -> "",
        "conflictParam" -> ""),
      paramorder = Seq(
        "measureParam",
        "priorCountParam",
        "priorCorrelParam",
        "minNumRatersParam",
        "maxNumRatersParam",
        "minIntersectionParam",
        "minNumRatedSimParam",
        "viewParam",
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      engineinfoid = "itemsim",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")))
    algoinfos.insert(AlgoInfo(
      id = "pdio-is2",
      name = "kNN Item Based Collaborative Filtering",
      description = Some("This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false")),
      paramdefaults = Map(
        "measureParam" -> "correl",
        "priorCountParam" -> 20,
        "priorCorrelParam" -> 0,
        "minNumRatersParam" -> 1,
        "maxNumRatersParam" -> 10000,
        "minIntersectionParam" -> 1,
        "minNumRatedSimParam" -> 1,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest",
        "priorCountParamMin" -> 10,
        "priorCountParamMax" -> 30,
        "priorCorrelParamMin" -> 0.0,
        "priorCorrelParamMax" -> 0.1,
        "minNumRatersParamMin" -> 1,
        "minNumRatersParamMax" -> 5,
        "maxNumRatersParamMin" -> 10000,
        "maxNumRatersParamMax" -> 10000,
        "minIntersectionParamMin" -> 1,
        "minIntersectionParamMax" -> 5,
        "minNumRatedSimParamMin" -> 1,
        "minNumRatedSimParamMax" -> 5
        ),
      paramnames = Map(
        "measureParam" -> "Distance Function",
        "priorCountParam" -> "Virtual Count",
        "priorCorrelParam" -> "Prior Correlation",
        "minNumRatersParam" -> "Minimum Number of Raters",
        "maxNumRatersParam" -> "Maximum Number of Raters",
        "minIntersectionParam" -> "Minimum Intersection",
        "minNumRatedSimParam" -> "Minimum Number of Rated Similar Items",
        "viewParam" -> "View Score",
        "viewmoreParam" -> "View More Score",
        "likeParam" -> "Like Score",
        "dislikeParam" -> "Dislike Score",
        "conversionParam" -> "Buy Score",
        "conflictParam" -> "Override"),
      paramdescription = Map(
        "measureParam" -> "",
        "priorCountParam" -> "Suggested range: 0 to 100.",
        "priorCorrelParam" -> "",
        "minNumRatersParam" -> "",
        "maxNumRatersParam" -> "",
        "minIntersectionParam" -> "",
        "minNumRatedSimParam" -> "",
        "viewParam" -> "",
        "viewmoreParam" -> "",
        "likeParam" -> "",
        "dislikeParam" -> "",
        "conversionParam" -> "",
        "conflictParam" -> ""),
      paramorder = Seq(
        "measureParam",
        "priorCountParam",
        "priorCorrelParam",
        "minNumRatersParam",
        "maxNumRatersParam",
        "minIntersectionParam",
        "minNumRatedSimParam",
        "viewParam",
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      engineinfoid = "itemsim",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")))
    algoinfos.getByEngineInfoId("itemsim").size must beEqualTo(2)
  }

  def update(algoinfos: AlgoInfos) = {
    val u1 = AlgoInfo(
      id = "pdio-u1",
      name = "kNN Item Based Collaborative Filtering",
      description = Some("This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false")),
      paramdefaults = Map(
        "measureParam" -> "correl",
        "priorCountParam" -> 20,
        "priorCorrelParam" -> 0,
        "minNumRatersParam" -> 1,
        "maxNumRatersParam" -> 10000,
        "minIntersectionParam" -> 1,
        "minNumRatedSimParam" -> 1,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest",
        "priorCountParamMin" -> 10,
        "priorCountParamMax" -> 30,
        "priorCorrelParamMin" -> 0.0,
        "priorCorrelParamMax" -> 0.1,
        "minNumRatersParamMin" -> 1,
        "minNumRatersParamMax" -> 5,
        "maxNumRatersParamMin" -> 10000,
        "maxNumRatersParamMax" -> 10000,
        "minIntersectionParamMin" -> 1,
        "minIntersectionParamMax" -> 5,
        "minNumRatedSimParamMin" -> 1,
        "minNumRatedSimParamMax" -> 5
        ),
      paramnames = Map(
        "measureParam" -> "Distance Function",
        "priorCountParam" -> "Virtual Count",
        "priorCorrelParam" -> "Prior Correlation",
        "minNumRatersParam" -> "Minimum Number of Raters",
        "maxNumRatersParam" -> "Maximum Number of Raters",
        "minIntersectionParam" -> "Minimum Intersection",
        "minNumRatedSimParam" -> "Minimum Number of Rated Similar Items",
        "viewParam" -> "View Score",
        "viewmoreParam" -> "View More Score",
        "likeParam" -> "Like Score",
        "dislikeParam" -> "Dislike Score",
        "conversionParam" -> "Buy Score",
        "conflictParam" -> "Override"),
      paramdescription = Map(
        "measureParam" -> "",
        "priorCountParam" -> "Suggested range: 0 to 100.",
        "priorCorrelParam" -> "",
        "minNumRatersParam" -> "",
        "maxNumRatersParam" -> "",
        "minIntersectionParam" -> "",
        "minNumRatedSimParam" -> "",
        "viewParam" -> "",
        "viewmoreParam" -> "",
        "likeParam" -> "",
        "dislikeParam" -> "",
        "conversionParam" -> "",
        "conflictParam" -> ""),
      paramorder = Seq(
        "measureParam",
        "priorCountParam",
        "priorCorrelParam",
        "minNumRatersParam",
        "maxNumRatersParam",
        "minIntersectionParam",
        "minNumRatedSimParam",
        "viewParam",
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      engineinfoid = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate."))
    algoinfos.insert(u1)

    val u2 = u1.copy(techreq = Seq("GraphLab"))
    algoinfos.update(u2)
    algoinfos.get(u2.id) must beSome(u2)
  }

  def delete(algoinfos: AlgoInfos) = {
    algoinfos.insert(AlgoInfo(
      id = "pdio-d1",
      name = "kNN Item Based Collaborative Filtering",
      description = Some("This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$ --numRecommendations $numRecommendations$ --unseenOnly $unseenOnly$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false")),
      paramdefaults = Map(
        "measureParam" -> "correl",
        "priorCountParam" -> 20,
        "priorCorrelParam" -> 0,
        "minNumRatersParam" -> 1,
        "maxNumRatersParam" -> 10000,
        "minIntersectionParam" -> 1,
        "minNumRatedSimParam" -> 1,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest",
        "priorCountParamMin" -> 10,
        "priorCountParamMax" -> 30,
        "priorCorrelParamMin" -> 0.0,
        "priorCorrelParamMax" -> 0.1,
        "minNumRatersParamMin" -> 1,
        "minNumRatersParamMax" -> 5,
        "maxNumRatersParamMin" -> 10000,
        "maxNumRatersParamMax" -> 10000,
        "minIntersectionParamMin" -> 1,
        "minIntersectionParamMax" -> 5,
        "minNumRatedSimParamMin" -> 1,
        "minNumRatedSimParamMax" -> 5
        ),
      paramnames = Map(
        "measureParam" -> "Distance Function",
        "priorCountParam" -> "Virtual Count",
        "priorCorrelParam" -> "Prior Correlation",
        "minNumRatersParam" -> "Minimum Number of Raters",
        "maxNumRatersParam" -> "Maximum Number of Raters",
        "minIntersectionParam" -> "Minimum Intersection",
        "minNumRatedSimParam" -> "Minimum Number of Rated Similar Items",
        "viewParam" -> "View Score",
        "viewmoreParam" -> "View More Score",
        "likeParam" -> "Like Score",
        "dislikeParam" -> "Dislike Score",
        "conversionParam" -> "Buy Score",
        "conflictParam" -> "Override"),
      paramdescription = Map(
        "measureParam" -> "",
        "priorCountParam" -> "Suggested range: 0 to 100.",
        "priorCorrelParam" -> "",
        "minNumRatersParam" -> "",
        "maxNumRatersParam" -> "",
        "minIntersectionParam" -> "",
        "minNumRatedSimParam" -> "",
        "viewParam" -> "",
        "viewmoreParam" -> "",
        "likeParam" -> "",
        "dislikeParam" -> "",
        "conversionParam" -> "",
        "conflictParam" -> ""),
      paramorder = Seq(
        "measureParam",
        "priorCountParam",
        "priorCorrelParam",
        "minNumRatersParam",
        "maxNumRatersParam",
        "minIntersectionParam",
        "minNumRatedSimParam",
        "viewParam",
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      engineinfoid = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")))
    algoinfos.delete("pdio-d1")
    algoinfos.get("pdio-d1") must beNone  
  }
}
