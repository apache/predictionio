package io.prediction.output.itemrank

import io.prediction.commons.Config
import io.prediction.commons.settings.{ Algo, App, Engine, OfflineEval }

import com.github.nscala_time.time.Imports._

object ItemRankAllItemOutput extends ItemRankAlgoOutput {
  override def output(uid: String, iids: Seq[String],
    instant: DateTime)(implicit app: App, algo: Algo,
      offlineEval: Option[OfflineEval]): Seq[(String, Double)] = {
    val config = new Config
    val itemRecScores = offlineEval map { _ =>
      config.getModeldataTrainingItemRecScores
    } getOrElse { config.getModeldataItemRecScores }
    val allItemsForUid = itemRecScores.getByUid(uid)

    /**
     * Rank based on scores. If no recommendations found for this user,
     * simply return the original list.
     */
    val n = iids.size
    val iidsSet = iids.toSet
    allItemsForUid map { allItems =>
      allItems.iids.zip(allItems.scores).filter(p => iidsSet(p._1)).sortBy(_._2)
        .reverse
    } getOrElse Seq[(String, Double)]()
  }
}
