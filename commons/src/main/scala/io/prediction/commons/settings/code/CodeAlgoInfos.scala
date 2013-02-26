package io.prediction.commons.settings.code

import io.prediction.commons.settings.{AlgoInfo, AlgoInfos}

/** Scala implementation of AlgoInfos. */
class CodeAlgoInfos extends AlgoInfos {
  private val algoInfos = Map(
    "io.prediction.algorithms.scalding.itemrec.knnitembased" -> AlgoInfo(
      id = "io.prediction.algorithms.scalding.itemrec.knnitembased",
      name = "kNN Item Based Collaborative Filtering",
      description = None,
      batchcommands = Some(Seq(
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$",
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq(
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfs --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --measureParam $measureParam$ --priorCountParam $priorCountParam$ --priorCorrelParam $priorCorrelParam$ --minNumRatersParam $minNumRatersParam$ --maxNumRatersParam $maxNumRatersParam$ --minIntersectionParam $minIntersectionParam$ --minNumRatedSimParam $minNumRatedSimParam$",
        "hadoop jar $jar$ io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor --hdfs --dbType file --dbName $modeldatadir$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet false")),
      defaultparams = Map(
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
        "conflictParam" -> "latest"), // latest, highest, lowest
      enginetype = "itemrec",
      datareq = Seq("u2iactions")
    )
  )

  private val engineTypeToAlgoInfos = Map("itemrec" -> Seq("io.prediction.algorithms.scalding.itemrec.knnitembased"))

  def get(id: String) = {
    algoInfos.get(id)
  }

  def getByEngineType(enginetype: String) = {
    engineTypeToAlgoInfos.getOrElse(enginetype, Seq()).map(algoInfos(_))
  }
}
