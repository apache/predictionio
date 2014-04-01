package io.prediction.algorithms.mahout.itemrec.knnitembased

import io.prediction.algorithms.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.similarity.ItemSimilarity
import org.apache.mahout.cf.taste.impl.recommender.{ GenericItemBasedRecommender, GenericBooleanPrefItemBasedRecommender }
import org.apache.mahout.cf.taste.impl.similarity.{
  CityBlockSimilarity,
  EuclideanDistanceSimilarity,
  LogLikelihoodSimilarity,
  PearsonCorrelationSimilarity,
  TanimotoCoefficientSimilarity,
  UncenteredCosineSimilarity
}
import org.apache.mahout.cf.taste.impl.similarity.file.FileItemSimilarity
import org.apache.mahout.cf.taste.similarity.precompute.BatchItemSimilarities
import org.apache.mahout.cf.taste.impl.similarity.precompute.MultithreadedBatchItemSimilarities
import org.apache.mahout.cf.taste.impl.similarity.precompute.FileSimilarItemsWriter
import org.apache.mahout.cf.taste.similarity.precompute.SimilarItems

import scala.collection.JavaConversions._

import java.io.File

class KNNItemBasedJob extends MahoutJob {

  val defaultItemSimilarity = "LogLikelihoodSimilarity"

  override def buildRecommender(dataModel: DataModel, args: Map[String, String]): Recommender = {

    val booleanData: Boolean = getArgOpt(args, "booleanData", "false").toBoolean
    val itemSimilarity: String = getArgOpt(args, "itemSimilarity", defaultItemSimilarity)
    val weighted: Boolean = getArgOpt(args, "weighted", "false").toBoolean
    val threshold: Double = getArgOpt(args, "threshold").map(_.toDouble).getOrElse(Double.MinPositiveValue)
    val nearestN: Int = getArgOpt(args, "nearestN", "10").toInt
    val outputSim: String = getArg(args, "outputSim")

    val preComputeItemSim: Boolean = getArgOpt(args, "preComputeItemSim", "true").toBoolean
    val similarItemsPerItem: Int = getArgOpt(args, "similarItemsPerItem", "100").toInt // number of similar items per item in pre-computation
    // MultithreadedBatchItemSimilarities parameter
    /*
    val batchSize: Int = getArgOpt(args, "batchSize", "500").toInt
    val degreeOfParallelism: Int = getArgOpt(args, "parallelism", "8").toInt
    val maxDurationInHours: Int = getArgOpt(args, "maxHours", "6").toInt
    */

    val weightedParam: Weighting = if (weighted) Weighting.WEIGHTED else Weighting.UNWEIGHTED

    val similarity: ItemSimilarity = itemSimilarity match {
      case "CityBlockSimilarity" => new CityBlockSimilarity(dataModel)
      case "EuclideanDistanceSimilarity" => new EuclideanDistanceSimilarity(dataModel, weightedParam)
      case "LogLikelihoodSimilarity" => new LogLikelihoodSimilarity(dataModel)
      case "PearsonCorrelationSimilarity" => new PearsonCorrelationSimilarity(dataModel, weightedParam)
      case "TanimotoCoefficientSimilarity" => new TanimotoCoefficientSimilarity(dataModel)
      case "UncenteredCosineSimilarity" => new UncenteredCosineSimilarity(dataModel, weightedParam)
      case _ => throw new RuntimeException("Invalid ItemSimilarity: " + itemSimilarity)
    }

    if (preComputeItemSim) {
      val outputSimFile = new File(outputSim)
      outputSimFile.getParentFile().mkdirs()
      // delete old file
      if (outputSimFile.exists()) outputSimFile.delete()

      /*
      val genericRecommeder = new GenericItemBasedRecommender(dataModel, similarity)
      val batch: BatchItemSimilarities = new MultithreadedBatchItemSimilarities(genericRecommeder, similarItemsPerItem, batchSize)
      batch.computeItemSimilarities(degreeOfParallelism, maxDurationInHours, new FileSimilarItemsWriter(outputSimFile))
      */

      val genericRecommeder = new GenericItemBasedRecommender(dataModel, similarity)
      val itemIds = dataModel.getItemIDs.toSeq

      val simPar = itemIds.par.map { itemId =>
        new SimilarItems(itemId, genericRecommeder.mostSimilarItems(itemId, similarItemsPerItem))
      }

      val writer = new FileSimilarItemsWriter(outputSimFile)
      writer.open()
      simPar.seq.foreach { s: SimilarItems =>
        writer.add(s)
      }
      writer.close()
    }

    val recSimilarity = if (preComputeItemSim) {
      new FileItemSimilarity(new File(outputSim))
    } else similarity

    val recommender: Recommender = new KNNItemBasedRecommender(dataModel, recSimilarity, booleanData, nearestN, threshold)

    recommender
  }

}