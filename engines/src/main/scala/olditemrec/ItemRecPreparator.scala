package io.prediction.engines.olditemrec

import io.prediction.controller._

// We reuse the sample business logic as in ItemRankPreparator. Will refactor
// this code into a common util package.

import io.prediction.engines.itemrank.PreparatorParams
import io.prediction.engines.itemrank.ItemRankPreparator
import io.prediction.engines.base.{ PreparedData => IRPreparedData }
import io.prediction.engines.base.{ TrainingData => IRTrainingData }

import org.apache.mahout.cf.taste.model.DataModel
import io.prediction.engines.util.MahoutUtil;

import io.prediction.engines.java.olditemrec.data.PreparedData


class NewItemRecPreparator(pp: PreparatorParams)
  extends LPreparator[
      PreparatorParams,
      IRTrainingData,
      PreparedData] {

  val irPreparator = new ItemRankPreparator(pp)

  override def prepare(irTrainingData: IRTrainingData): PreparedData = {
    val irPreparedData: IRPreparedData = irPreparator.prepare(irTrainingData)

    val ratings: Seq[(Int, Int, Float, Long)] = irPreparedData.rating
    .map { r => (r.uindex, r.iindex, r.rating.toFloat, r.t) }

    val dataModel = MahoutUtil.buildDataModel(ratings)
    new PreparedData(dataModel)
  }

}
