package io.prediction.algorithms.mahout.itemrec.thresholduserbased

import java.io.File
import java.io.FileWriter

import scala.collection.JavaConversions._

import io.prediction.commons.filepath.{DataFile, AlgoFile}
import io.prediction.commons.Config
import io.prediction.commons.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.recommender.RecommendedItem
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood
import org.apache.mahout.cf.taste.similarity.UserSimilarity
import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.impl.recommender.{GenericUserBasedRecommender, GenericBooleanPrefUserBasedRecommender}
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood
import org.apache.mahout.cf.taste.impl.similarity.{CityBlockSimilarity, EuclideanDistanceSimilarity, LogLikelihoodSimilarity, 
  PearsonCorrelationSimilarity, SpearmanCorrelationSimilarity, TanimotoCoefficientSimilarity, UncenteredCosineSimilarity}

class ThresholdUserBasedJob extends MahoutJob {
  
  val userSimilarityValues = Seq(
    "CityBlockSimilarity", 
    "EuclideanDistanceSimilarity",
    "LogLikelihoodSimilarity",
    "PearsonCorrelationSimilarity",
    "SpearmanCorrelationSimilarity",
    "TanimotoCoefficientSimilarity",
    "UncenteredCosineSimilarity")

  val defaultUserSimilarity = "PearsonCorrelationSimilarity"

  override def run(args: Map[String, String]) = {

    println("Running job with args: " + args)

    val input = args("input")
    val output = args("output")
    val booleanData: Boolean = getArgOpt(args, "booleanData", "false").toBoolean
    val numRecommendations: Int = getArgOpt(args, "numRecommendations", "10").toInt
    val userSimilarity: String = getArgOpt(args, "userSimilarity", defaultUserSimilarity)
    val weighted: Boolean = getArgOpt(args, "weighted", "false").toBoolean
    val threshold: Double = getArgOpt(args, "threshold").map( _.toDouble).getOrElse(Double.MinPositiveValue)
    val samplingRate: Double = getArgOpt(args, "samplingRate", "1.0").toDouble

    val model: DataModel = new FileDataModel(new File(input))
      
    val weightedParam: Weighting = if (weighted) Weighting.WEIGHTED else Weighting.UNWEIGHTED

    val similarity: UserSimilarity = userSimilarity match {
      case "CityBlockSimilarity" => new CityBlockSimilarity(model)
      case "EuclideanDistanceSimilarity" => new EuclideanDistanceSimilarity(model, weightedParam)
      case "LogLikelihoodSimilarity" => new LogLikelihoodSimilarity(model)
      case "PearsonCorrelationSimilarity" => new PearsonCorrelationSimilarity(model, weightedParam)
      case "SpearmanCorrelationSimilarity" => new SpearmanCorrelationSimilarity(model)
      case "TanimotoCoefficientSimilarity" => new TanimotoCoefficientSimilarity(model)
      case "UncenteredCosineSimilarity" => new UncenteredCosineSimilarity(model, weightedParam)
      case _ => throw new RuntimeException("Invalid UserSimilarity: " + userSimilarity)
    }

    val neighborhood: UserNeighborhood = new ThresholdUserNeighborhood(threshold, similarity, model, samplingRate)

    val recommender: Recommender = if (booleanData) {
      new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity)
    } else {
      new GenericUserBasedRecommender(model, neighborhood, similarity)
    }

    // generate prediction output file
    
    val outputWriter = new FileWriter(new File(output))
    
    val userIds = model.getUserIDs

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
