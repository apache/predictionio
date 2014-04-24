package io.prediction.output.itemrec

import io.prediction.commons.Config
import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.settings.{ Algo, App, OfflineEval }
import com.github.nscala_time.time.Imports._

object UserProfileRecommendationRealtimeOutput {
  private val config = new Config
  def output(uid: String, n: Int, itypes: Option[Seq[String]], instant: DateTime)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]): Iterator[String] = {
    // We ignore the parameter n here, since some items are filtered.
    val itemRecScores = config.getModeldataItemRecScores
    val itemRecScore = itemRecScores.getByUid(uid).get

    val metadataKeyvals = config.getModeldataMetadataKeyvals
    val optionalFeatureStr = metadataKeyvals.get(algo.id, algo.modelset, "features")
    if (optionalFeatureStr.isEmpty) {
      // When feature list not found in metadata, cannot recommendend items.
      // TODO: Return better error message.
      return Seq[String]().iterator
    }
    val features = optionalFeatureStr.get.split(',')
    val featureScores = itemRecScore.scores
    val featureScoreMap = features.zip(featureScores).toMap

    val items = config.getAppdataItems
    val itemList = items.getByAppidAndItypesAndTime(app.id, itypes,
      Some(instant)).toSeq

    val itemScoreList = itemList.map { item =>
      {
        val score = item.itypes.map { itype =>
          featureScoreMap.getOrElse(itype, 0).asInstanceOf[Double]
        }.sum
        (item, score)
      }
    }

    itemScoreList.sortBy(_._2).map(_._1.id).iterator
  }
}

object ItemRecAlgoRealtimeOutput {
  private val config = new Config

  def output(uid: String, n: Int, itypes: Option[Seq[String]],
    instant: DateTime)(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]) = {
    // Not handling offline eval
    UserProfileRecommendationRealtimeOutput.output(uid, n, itypes, instant)
  }
}
