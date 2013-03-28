package io.prediction.commons.settings.code

import io.prediction.commons.settings.{AlgoInfo, AlgoInfos}

/** Scala implementation of AlgoInfos. */
class CodeAlgoInfos extends AlgoInfos {
  private val wipAlgoInfos = Map(
    "pdio-knnitembased" -> AlgoInfo(
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
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "measureParam" -> ("Distance Function", ""),
        "priorCountParam" -> ("Virtual Count", "Suggested range: 0 to 100."),
        "priorCorrelParam" -> ("Prior Correlation", ""),
        "minNumRatersParam" -> ("Minimum Number of Raters", ""),
        "maxNumRatersParam" -> ("Maximum Number of Raters", ""),
        "minIntersectionParam" -> ("Minimum Intersection", ""),
        "minNumRatedSimParam" -> ("Minimum Number of Rated Similar Items", ""),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "measureParam",
        "priorCountParam",
        "priorCorrelParam",
        "minNumRatersParam",
        "maxNumRatersParam",
        "minIntersectionParam",
        "minNumRatedSimParam",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-itembased" -> AlgoInfo(
      id = "mahout-itembased",
      name = "Mahout's Item Based Collaborative Filtering",
      description = Some("Predicts user preferences based on previous behaviors of users on similar items."),
      batchcommands = Some(Seq(
        "$base$/bin/quiet.sh $hadoop$ fs -rmr $mahoutTempDir$",
        "$base$/bin/quiet.sh $hadoop$ fs -rmr $algoDir$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $mahoutCoreJobJar$ org.apache.mahout.cf.taste.hadoop.item.RecommenderJob --input $dataFilePrefix$ratings.csv --output $algoFilePrefix$predicted.tsv --tempDir $mahoutTempDir$ --similarityClassname $similarityClassname$ --numRecommendations $numRecommendations$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      offlineevalcommands = Some(Seq(
        "$base$/bin/quiet.sh $hadoop$ fs -rmr $mahoutTempDir$",
        "$base$/bin/quiet.sh $hadoop$ fs -rmr $algoDir$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $mahoutCoreJobJar$ org.apache.mahout.cf.taste.hadoop.item.RecommenderJob --input $dataFilePrefix$ratings.csv --output $algoFilePrefix$predicted.tsv --tempDir $mahoutTempDir$ --similarityClassname $similarityClassname$ --numRecommendations $numRecommendations$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      paramdefaults = Map(
        "similarityClassname" -> "SIMILARITY_COOCCURRENCE",
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "similarityClassname" -> ("Distance Function", ""),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "similarityClassname",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-parallelals" -> AlgoInfo(
      id = "mahout-parallelals",
      name = "Mahout's Parallel ALS-WR",
      description = Some("Predicts user preferences based on previous behaviors of users."),
      batchcommands = Some(Seq(
        "$base$/bin/quiet.sh $hadoop$ fs -rmr $mahoutTempDir$",
        "$base$/bin/quiet.sh $hadoop$ fs -rmr $algoDir$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $mahoutCoreJobJar$ org.apache.mahout.cf.taste.hadoop.als.ParallelALSFactorizationJob --input $dataFilePrefix$ratings.csv --output $algoFilePrefix$matrix --tempDir $mahoutTempDir$ --lambda $lambda$ --implicitFeedback $implicitFeedback$ --numFeatures $numFeatures$ --numIterations $numIterations$",
        "$hadoop$ jar $mahoutCoreJobJar$ org.apache.mahout.cf.taste.hadoop.als.RecommenderJob --input $algoFilePrefix$matrix/userRatings --userFeatures $algoFilePrefix$matrix/U --itemFeatures $algoFilePrefix$matrix/M --output $algoFilePrefix$predicted.tsv --tempDir $mahoutTempDir$ --numRecommendations $numRecommendations$ --maxRating 5",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      offlineevalcommands = Some(Seq(
        "$base$/bin/quiet.sh $hadoop$ fs -rmr $mahoutTempDir$",
        "$base$/bin/quiet.sh $hadoop$ fs -rmr $algoDir$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $mahoutCoreJobJar$ org.apache.mahout.cf.taste.hadoop.als.ParallelALSFactorizationJob --input $dataFilePrefix$ratings.csv --output $algoFilePrefix$matrix --tempDir $mahoutTempDir$ --lambda $lambda$ --implicitFeedback $implicitFeedback$ --numFeatures $numFeatures$ --numIterations $numIterations$",
        "$hadoop$ jar $mahoutCoreJobJar$ org.apache.mahout.cf.taste.hadoop.als.RecommenderJob --input $algoFilePrefix$matrix/userRatings --userFeatures $algoFilePrefix$matrix/U --itemFeatures $algoFilePrefix$matrix/M --output $algoFilePrefix$predicted.tsv --tempDir $mahoutTempDir$ --numRecommendations $numRecommendations$ --maxRating 5",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      paramdefaults = Map(
        "lambda" -> 0.03,
        "implicitFeedback" -> false,
        "alpha" -> 40,
        "numFeatures" -> 3,
        "numIterations" -> 5,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "lambda" -> ("Lambda", "Regularization param to avoid overfitting."),
        "implicitFeedback" -> ("Implicit Feedback", "Whether data consists of implicit data."),
        "alpha" -> ("Alpha", "Confidence param (will be ignored if Implicit Feedback is false)."),
        "numFeatures" -> ("Num of Factorized Features", "Dimension of the factorized feature space."),
        "numIterations" -> ("Number of Iterations", "Number of training iteration."),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "lambda",
        "implicitFeedback",
        "alpha",
        "numFeatures",
        "numIterations",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-knnuserbased" -> AlgoInfo(
      id = "mahout-knnuserbased",
      name = "Mahout's kNN User Based Collaborative Filtering (Non-distributed)",
      description = Some("Predicts user preferences based on previous behaviors of users who are the k-nearest neighbors (Non-distributed)."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.knnuserbased.KNNUserBasedJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --booleanData $booleanData$ --numRecommendations $numRecommendations$ --nearestN $nearestN$ --userSimilarity $userSimilarity$ --weighted $weighted$ --minSimilarity $minSimilarity$ --samplingRate $samplingRate$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.knnuserbased.KNNUserBasedJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --booleanData $booleanData$ --numRecommendations $numRecommendations$ --nearestN $nearestN$ --userSimilarity $userSimilarity$ --weighted $weighted$ --minSimilarity $minSimilarity$ --samplingRate $samplingRate$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      paramdefaults = Map(
        "booleanData" -> false,
        "nearestN" -> 10,
        "userSimilarity" -> "PearsonCorrelationSimilarity",
        "weighted" -> false,
        "minSimilarity" -> -1.0,
        "samplingRate" -> 1.0,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "booleanData" -> ("Boolean Data", "Treat input data as having no preference values."),
        "nearestN" -> ("Nearest K", "K-nearest neighbors to a given user."),
        "userSimilarity" -> ("User Similarity", "User Similarity Measure."),
        "weighted" -> ("Weighted", "The Similarity score is weighted (only applied to Euclidean Distance, Pearson Correlation, Uncentered Cosine user similarity)."),
        "minSimilarity" -> ("Minimal Similarity", "Minimal similarity required for neighbors."),
        "samplingRate" -> ("Sampling Rate", "Must be greater > 0 and <= 1. Percentage of users to consider when building neighborhood. Decrease to trade quality for performance."),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "booleanData",
        "nearestN",
        "userSimilarity",
        "weighted",
        "minSimilarity",
        "samplingRate",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-thresholduserbased" -> AlgoInfo(
      id = "mahout-thresholduserbased",
      name = "Mahout's Threshold User Based Collaborative Filtering (Non-distributed)",
      description = Some("Predicts user preferences based on previous behaviors of users whose similarity meets or exceeds a certain threshold (Non-distributed)."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.thresholduserbased.ThresholdUserBasedJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --booleanData $booleanData$ --numRecommendations $numRecommendations$ --threshold $threshold$ --userSimilarity $userSimilarity$ --weighted $weighted$ --samplingRate $samplingRate$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.thresholduserbased.ThresholdUserBasedJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --booleanData $booleanData$ --numRecommendations $numRecommendations$ --threshold $threshold$ --userSimilarity $userSimilarity$ --weighted $weighted$ --samplingRate $samplingRate$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      paramdefaults = Map(
        "booleanData" -> false,
        "threshold" -> 0.0001,
        "userSimilarity" -> "PearsonCorrelationSimilarity",
        "weighted" -> false,
        "samplingRate" -> 1.0,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "booleanData" -> ("Boolean Data", "Treat input data as having no preference values."),
        "threshold" -> ("Threshold", "Similarity threshold."),
        "userSimilarity" -> ("User Similarity", "User Similarity Measure."),
        "weighted" -> ("Weighted", "The Similarity score is weighted (only applied to Euclidean Distance, Pearson Correlation, Uncentered Cosine user similarity)."),
        "samplingRate" -> ("Sampling Rate", "Must be greater > 0 and <= 1. Percentage of users to consider when building neighborhood. Decrease to trade quality for performance."),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "booleanData",
        "threshold",
        "userSimilarity",
        "weighted",
        "samplingRate",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-slopeone" -> AlgoInfo(
      id = "mahout-slopeone",
      name = "Mahout's SlopeOne Rating Based Collaborative Filtering (Non-distributed)",
      description = Some("Predicts user preferences based on average difference in preference values between new items and the items for which the user has indicated preferences (Non-distributed)."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.slopeone.SlopeOneJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --numRecommendations $numRecommendations$ --weighting $weighting$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.slopeone.SlopeOneJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --numRecommendations $numRecommendations$ --weighting $weighting$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      paramdefaults = Map(
        "weighting" -> "Standard_Deviation",
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "weighting" -> ("Weighting", "Weighted preference difference."),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "weighting",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-alswr" -> AlgoInfo(
      id = "mahout-alswr",
      name = "Mahout's ALS-WR (Non-distributed)",
      description = Some("Predict user preferences using matrix factorization (Non-distributed)."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.alswr.ALSWRJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --numRecommendations $numRecommendations$ --numFeatures $numFeatures$ --lambda $lambda$ --numIterations $numIterations$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.alswr.ALSWRJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --numRecommendations $numRecommendations$ --numFeatures $numFeatures$ --lambda $lambda$ --numIterations $numIterations$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      paramdefaults = Map(
        "numFeatures" -> 3,
        "lambda" -> 0.03,
        "numIterations" -> 3,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "numFeatures" -> ("Num of Factorized Features", "Dimension of the factorized feature space."),
        "lambda" -> ("Lambda", "Regularization param to avoid overfitting."),
        "numIterations" -> ("Number of Iterations", "Number of training iteration."),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "numFeatures",
        "lambda",
        "numIterations",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-svdsgd" -> AlgoInfo(
      id = "mahout-svdsgd",
      name = "Mahout's SVD-RatingSGD Recommender (Non-distributed)",
      description = Some("Predict user preferences using matrix factorization (Non-distributed)."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.svdsgd.SVDSGDJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --numRecommendations $numRecommendations$ --numFeatures $numFeatures$ --learningRate $learningRate$ --preventOverfitting $preventOverfitting$ --randomNoise $randomNoise$ --numIterations $numIterations$ --learningRateDecay $learningRateDecay$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.svdsgd.SVDSGDJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --numRecommendations $numRecommendations$ --numFeatures $numFeatures$ --learningRate $learningRate$ --preventOverfitting $preventOverfitting$ --randomNoise $randomNoise$ --numIterations $numIterations$ --learningRateDecay $learningRateDecay$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      paramdefaults = Map(
        "numFeatures" -> 3,
        "learningRate" -> 0.01,
        "preventOverfitting" -> 0.1,
        "randomNoise" -> 0.01,
        "numIterations" -> 3,
        "learningRateDecay" -> 1.0,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "numFeatures" -> ("Num of Factorized Features", "Dimension of the factorized feature space."),
        "learningRate" -> ("Learning Rate", ""),
        "preventOverfitting" -> ("Prevent Overfitting", ""),
        "randomNoise" -> ("Random Noise", ""),
        "numIterations" -> ("Number of Iterations", "Number of training iteration."),
        "learningRateDecay" -> ("Learning Rate Decay", ""),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "numFeatures",
        "learningRate",
        "preventOverfitting",
        "randomNoise",
        "numIterations",
        "learningRateDecay",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),
    "mahout-svdplusplus" -> AlgoInfo(
      id = "mahout-svdplusplus",
      name = "Mahout's SVDPlusPlus Recommender (Non-distributed)",
      description = Some("Predict user preferences using matrix factorization (Non-distributed)."),
      batchcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataDbType$ --dbName $appdataDbName$ --dbHost $appdataDbHost$ --dbPort $appdataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.svdplusplus.SVDPlusPlusJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --numRecommendations $numRecommendations$ --numFeatures $numFeatures$ --learningRate $learningRate$ --preventOverfitting $preventOverfitting$ --randomNoise $randomNoise$ --numIterations $numIterations$ --learningRateDecay $learningRateDecay$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataDbType$ --dbName $modeldataDbName$ --dbHost $modeldataDbHost$ --dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      offlineevalcommands = Some(Seq(
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataCopy --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.DataPreparator --hdfs --dbType $appdataTrainingDbType$ --dbName $appdataTrainingDbName$ --dbHost $appdataTrainingDbHost$ --dbPort $appdataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ $itypes$ --viewParam $viewParam$ --likeParam $likeParam$ --dislikeParam $dislikeParam$ --conversionParam $conversionParam$ --conflictParam $conflictParam$",
        "java -Dio.prediction.base=$base$ $configFile$ -jar $itemrecScalaMahoutJar$ io.prediction.algorithms.mahout.itemrec.svdplusplus.SVDPlusPlusJob --hdfsRoot $hdfsRoot$ --localTempRoot $localTempRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --numRecommendations $numRecommendations$ --numFeatures $numFeatures$ --learningRate $learningRate$ --preventOverfitting $preventOverfitting$ --randomNoise $randomNoise$ --numIterations $numIterations$ --learningRateDecay $learningRateDecay$",
        "$hadoop$ jar $jar$ io.prediction.algorithms.scalding.mahout.itemrec.ModelConstructor --hdfs --dbType $modeldataTrainingDbType$ --dbName $modeldataTrainingDbName$ --dbHost $modeldataTrainingDbHost$ --dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --evalid $evalid$ --modelSet $modelset$ --unseenOnly $unseenOnly$ --numRecommendations $numRecommendations$")),
      paramdefaults = Map(
        "numFeatures" -> 3,
        "learningRate" -> 0.01,
        "preventOverfitting" -> 0.1,
        "randomNoise" -> 0.01,
        "numIterations" -> 3,
        "learningRateDecay" -> 1.0,
        "viewParam" -> 3,
        "viewmoreParam" -> 3,
        "likeParam" -> 5,
        "dislikeParam" -> 1,
        "conversionParam" -> 4,
        "conflictParam" -> "latest"), // latest, highest, lowest
      paramdescription = Map(
        "numFeatures" -> ("Num of Factorized Features", "Dimension of the factorized feature space."),
        "learningRate" -> ("Learning Rate", ""),
        "preventOverfitting" -> ("Prevent Overfitting", ""),
        "randomNoise" -> ("Random Noise", ""),
        "numIterations" -> ("Number of Iterations", "Number of training iteration."),
        "learningRateDecay" -> ("Learning Rate Decay", ""),
        "viewParam" -> ("View Score", ""),
        "viewmoreParam" -> ("View More Score", ""),
        "likeParam" -> ("Like Score", ""),
        "dislikeParam" -> ("Dislike Score", ""),
        "conversionParam" -> ("Buy Score", ""),
        "conflictParam" -> ("Override", "")),
      paramorder = Seq(
        "numFeatures",
        "learningRate",
        "preventOverfitting",
        "randomNoise",
        "numIterations",
        "learningRateDecay",
        "viewParam",
        //"viewmoreParam", // not visible for now
        "likeParam",
        "dislikeParam",
        "conversionParam",
        "conflictParam"),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users, Items, and U2I Actions such as Like, Buy and Rate.")
    ),

    "pdio-randomrank" -> AlgoInfo(
      id = "pdio-randomrank",
      name = "Random Rank",
      description = Some("Predict user preferences randomly."),
      batchcommands = Some(Seq("$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.randomrank.RandomRank --hdfs --training_dbType $appdataDbType$ --training_dbName $appdataDbName$ --training_dbHost $appdataDbHost$ --training_dbPort $appdataDbPort$ --modeldata_dbType $modeldataDbType$ --modeldata_dbName $modeldataDbName$ --modeldata_dbHost $modeldataDbHost$ --modeldata_dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --numRecommendations $numRecommendations$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq("$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.randomrank.RandomRank --hdfs --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --numRecommendations $numRecommendations$ --modelSet false --evalid $evalid$")),
      paramdefaults = Map(),
      paramdescription = Map(),
      paramorder = Seq(),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users and Items.")
    ),
    "pdio-latestrank" -> AlgoInfo(
      id = "pdio-latestrank",
      name = "Latest Rank",
      description = Some("Recommend latest items to users."),
      batchcommands = Some(Seq("$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.latestrank.LatestRank --hdfs --training_dbType $appdataDbType$ --training_dbName $appdataDbName$ --training_dbHost $appdataDbHost$ --training_dbPort $appdataDbPort$ --modeldata_dbType $modeldataDbType$ --modeldata_dbName $modeldataDbName$ --modeldata_dbHost $modeldataDbHost$ --modeldata_dbPort $modeldataDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet $modelset$")),
      offlineevalcommands = Some(Seq("$hadoop$ jar $jar$ io.prediction.algorithms.scalding.itemrec.latestrank.LatestRank --hdfs --training_dbType $appdataTrainingDbType$ --training_dbName $appdataTrainingDbName$ --training_dbHost $appdataTrainingDbHost$ --training_dbPort $appdataTrainingDbPort$ --modeldata_dbType $modeldataTrainingDbType$ --modeldata_dbName $modeldataTrainingDbName$ --modeldata_dbHost $modeldataTrainingDbHost$ --modeldata_dbPort $modeldataTrainingDbPort$ --hdfsRoot $hdfsRoot$ --appid $appid$ --engineid $engineid$ --algoid $algoid$ --modelSet false --evalid $evalid$")),
      paramdefaults = Map(),
      paramdescription = Map(),
      paramorder = Seq(),
      enginetype = "itemrec",
      techreq = Seq("Hadoop"),
      datareq = Seq("Users and Items with starttime.")
    )
  )

  /** Temporarily add alias before generalization is finished. */
  private val algoInfos = wipAlgoInfos ++ Map("io.prediction.algorithms.scalding.itemrec.knnitembased" -> wipAlgoInfos("pdio-knnitembased"))

  private val engineTypeToAlgoInfos = Map("itemrec" -> Seq(
    "pdio-knnitembased",
    "mahout-itembased",
    "mahout-parallelals",
    "mahout-knnuserbased",
    "mahout-thresholduserbased",
    "mahout-slopeone",
    "mahout-alswr",
    "mahout-svdsgd",
    "mahout-svdplusplus",
    "pdio-randomrank",
    "pdio-latestrank"))

  def get(id: String) = {
    algoInfos.get(id)
  }

  def getByEngineType(enginetype: String) = {
    engineTypeToAlgoInfos.getOrElse(enginetype, Seq()).map(algoInfos(_))
  }
}
