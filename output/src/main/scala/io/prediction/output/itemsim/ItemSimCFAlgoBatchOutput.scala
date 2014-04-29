package io.prediction.output.itemsim

import io.prediction.commons.Config
import io.prediction.commons.modeldata.{ ItemSimScore, ItemSimScores }
import io.prediction.commons.settings.{ Algo, App, OfflineEval }

import breeze.stats.{ mean, variance }
import scala.math

object ItemSimCFAlgoBatchOutput {
  private val config = new Config

  def combinedOutput(itemSimScores: ItemSimScores, iidList: Seq[String],
    n: Int, itypes: Option[Seq[String]])(
      implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]) = {
    val iidScoreList = iidList.map { iid =>
      {
        val itemScores = itemSimScores.getTopNIidsAndScores(iid, 0, itypes)
        val scores = itemScores.map(_._2)
        val meanScore = mean(scores)
        val stdevScore = math.sqrt(variance(scores))

        val itemStdScoreList = itemScores.map {
          case (item, score) => (item, (score - meanScore) / stdevScore)
        }
        itemStdScoreList
      }
    }.flatten

    // Sum score group by iid, then sort by score in descending order
    iidScoreList.groupBy(_._1).mapValues(_.map(_._2).sum).toSeq.sortBy(-_._2)
  }

  // iid can be a comma-delimited list of iids. In such case, this function
  // takes a union of all the simliar items and sorts by standardized score.
  def output(iid: String, n: Int, itypes: Option[Seq[String]])(
    implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]): Seq[(String, Double)] = {
    val itemSimScores = offlineEval map { _ =>
      config.getModeldataTrainingItemSimScores
    } getOrElse config.getModeldataItemSimScores

    //itemSimScores.getTopNIidsAndScores(iid, n, itypes)
    val iidList = iid.split(',')
    if (iidList.length == 1) {
      itemSimScores.getTopNIidsAndScores(iid, n, itypes)
    } else {
      combinedOutput(itemSimScores, iidList, n, itypes)
    }
  }
}
