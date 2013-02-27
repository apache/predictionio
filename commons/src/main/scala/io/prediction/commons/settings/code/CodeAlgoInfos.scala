package io.prediction.commons.settings.code

import io.prediction.commons.settings.{AlgoInfo, AlgoInfos}

/** Scala implementation of AlgoInfos. */
class CodeAlgoInfos extends AlgoInfos {
  private val algoInfos = Map(
    "pdio-knnitembased" -> AlgoInfo(
      id = "pdio-knnitembased",
      pkgname = "io.prediction.algorithms.scalding.itemrec.knnitembased",
      name = "kNN Item Based Collaborative Filtering",
      description = Some("This item-based k-NearestNeighbor algorithm predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq(
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$",
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq(
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$",
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false")),
      defaultparams = Seq(
        ("Distance Function", "measureParam", true) -> "correl",
        ("Virtual Count", "priorCountParam", true) -> 20,
        ("Prior Correlation", "priorCorrelParam", true) -> 0,
        ("Minimum Number of Raters", "minNumRatersParam", true) -> 1,
        ("Maximum Number of Raters", "maxNumRatersParam", true) -> 10000,
        ("Minimum Intersection", "minIntersectionParam", true) -> 1,
        ("Minimum Number of Rated Similar Items", "minNumRatedSimParam", true) -> 1,
        ("View Score", "viewParam", true) -> 3,
        ("View More Score", "viewmoreParam", false) -> 3,
        ("Like Score", "likeParam", true) -> 5,
        ("Dislike Score", "dislikeParam", true) -> 1,
        ("Buy Score", "conversionParam", true) -> 4,
        ("Override", "conflictParam", true) -> "latest"), // latest, highest, lowest
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-itembased" -> AlgoInfo(
      id = "mahout-itembased",
      pkgname = "TODO",
      name = "Mahout's Item Based Recommendation",
      description = Some("Predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq("TODO")),
      offlineevalcommands = Some(Seq("TODO")),
      defaultparams = Seq(
        ("View Score", "viewParam", true) -> 3,
        ("View More Score", "viewmoreParam", false) -> 3,
        ("Like Score", "likeParam", true) -> 5,
        ("Dislike Score", "dislikeParam", true) -> 1,
        ("Buy Score", "conversionParam", true) -> 4,
        ("Override", "conflictParam", true) -> "latest"), // latest, highest, lowest
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-parallelals" -> AlgoInfo(
      id = "mahout-parallelals",
      pkgname = "TODO",
      name = "Mahout's Parallel ALS",
      description = Some("Predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq("TODO")),
      offlineevalcommands = Some(Seq("TODO")),
      defaultparams = Seq(
        ("View Score", "viewParam", true) -> 3,
        ("View More Score", "viewmoreParam", false) -> 3,
        ("Like Score", "likeParam", true) -> 5,
        ("Dislike Score", "dislikeParam", true) -> 1,
        ("Buy Score", "conversionParam", true) -> 4,
        ("Override", "conflictParam", true) -> "latest"), // latest, highest, lowest
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("U2I Actions such as Like, Buy and Rate.")
    ),
    "pdio-randomrank" -> AlgoInfo(
      id = "pdio-randomrank",
      pkgname = "TODO",
      name = "Random Rank",
      description = Some("Predict user preferences randomly."),
      batchcommands = Some(Seq("TODO")),
      offlineevalcommands = Some(Seq("TODO")),
      defaultparams = Seq(),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users and Items.")
    ),
    "pdio-latestrank" -> AlgoInfo(
      id = "pdio-latestrank",
      pkgname = "TODO",
      name = "Latest Rank",
      description = Some("Recommend latest items to users."),
      batchcommands = Some(Seq("TODO")),
      offlineevalcommands = Some(Seq("TODO")),
      defaultparams = Seq(),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users and Items with starttime.")
    )
  )

  private val engineTypeToAlgoInfos = Map("itemrec" -> Seq("pdio-knnitembased", "mahout-itembased", "mahout-parallelals", "pdio-randomrank", "pdio-latestrank"))

  def get(id: String) = {
    algoInfos.get(id)
  }

  def getByEngineType(enginetype: String) = {
    engineTypeToAlgoInfos.getOrElse(enginetype, Seq()).map(algoInfos(_))
  }
}
