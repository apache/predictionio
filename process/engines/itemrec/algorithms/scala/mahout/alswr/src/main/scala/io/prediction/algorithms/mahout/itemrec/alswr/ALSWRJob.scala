package io.prediction.algorithms.mahout.itemrec.alswr

import java.io.File
import java.io.FileWriter

import scala.collection.JavaConversions._

import io.prediction.commons.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel
import org.apache.mahout.cf.taste.recommender.Recommender

import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender
import org.apache.mahout.cf.taste.impl.recommender.svd.ALSWRFactorizer
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer

class ALSWRJob extends MahoutJob {

  override def run(args: Map[String, String]) = {

    val input = args("input")
    val output = args("output")
    val numRecommendations: Int = getArgOpt(args, "numRecommendations", "10").toInt
    val numFeatures: Int = args("numFeatures").toInt
    val lambda: Double = args("lambda").toDouble
    val numIterations: Int = args("numIterations").toInt

    val dataModel: DataModel = new FileDataModel(new File(input))

    val factorizer: Factorizer = new ALSWRFactorizer(dataModel, numFeatures, lambda, numIterations)

    // default stratagy is PreferredItemsNeighborhoodCandidateItemsStrategy();
    val recommender: Recommender = new SVDRecommender(dataModel, factorizer)

    // generate prediction output file

    val outputWriter = new FileWriter(new File(output))

    val userIds = dataModel.getUserIDs

    while (userIds.hasNext) {
      val uid = userIds.next
      val rec = recommender.recommend(uid, numRecommendations)
      if (rec.size != 0) {
        val prediction = uid+"\t"+"[" + (rec map {x => x.getItemID +":"+x.getValue }).mkString(",") + "]"
        outputWriter.write(prediction+"\n")
      }
    }

    outputWriter.close()

    args

  }

}