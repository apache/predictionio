package io.prediction.algorithms.mahout.itemrec.genericuserbased

//import java.util.List
import java.io.File
import java.io.FileWriter

import scala.collection.JavaConversions._

import grizzled.slf4j.Logger

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.recommender.RecommendedItem
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood
import org.apache.mahout.cf.taste.similarity.UserSimilarity
import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.impl.recommender.{GenericUserBasedRecommender, GenericBooleanPrefUserBasedRecommender}
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood
import org.apache.mahout.cf.taste.impl.similarity.{CityBlockSimilarity, EuclideanDistanceSimilarity, LogLikelihoodSimilarity, 
  PearsonCorrelationSimilarity, SpearmanCorrelationSimilarity, TanimotoCoefficientSimilarity, UncenteredCosineSimilarity}

object GenericUserBased {
  
  val userSimilarityValues = Seq(
    "CityBlockSimilarity", 
    "EuclideanDistanceSimilarity",
    "LogLikelihoodSimilarity",
    "PearsonCorrelationSimilarity",
    "SpearmanCorrelationSimilarity",
    "TanimotoCoefficientSimilarity",
    "UncenteredCosineSimilarity")

  val defaultUserSimilarity = "PearsonCorrelationSimilarity"

  case class Config(
    input: String = "ratings.csv",
    output: String = "predicted.tsv",
    //evalid: Option[Int] = None,
    booleanData: Boolean = false,
    numRecommendations: Int = 10,
    nearestN: Int = 10,
    userSimilarity: String = defaultUserSimilarity, 
    weighted: Boolean = false,
    minSimilarity: Option[Double] = None,
    samplingRate: Double = 1.0
  )

  def main(args: Array[String]) {

    val logger = Logger(GenericUserBased.getClass)

    val parser = new scopt.immutable.OptionParser[Config]("GenericUserBased", "") { def options = Seq(
      opt(None, "input", "<File Name>", "User preference input file (default ratings.csv).") { 
        (v: String, c: Config) => c.copy(input = v) },
      opt(None, "output", "<File Name>", "Recommendation output file (default predicted.tsv).") { 
        (v: String, c: Config) => c.copy(output = v) },
      //intOpt(None, "evalid", "<int>", "Offline Eval Id.") {
      //  (v: Int, c: Config) => c.copy(evalid = Some(v))},
      booleanOpt(None, "booleanData", "<true/false>", "Treat input data as having no preference values (Default false).") { 
        (v: Boolean, c: Config) => c.copy(booleanData = v) },
      intOpt(None, "numRecommendations", "<int>", "Number of recommendations to compute per user (default 10).") { 
        (v: Int, c: Config) => c.copy(numRecommendations = v) }, 
      intOpt(None, "nearestN", "<int>", "Nearest n users to a given user (default 10).") { 
        (v: Int, c: Config) => c.copy(nearestN = v) },
      opt(None, "userSimilarity", "<User Similarity Measure>", "User Similarity Measures: " + 
        userSimilarityValues.mkString(", ") + ". (Default " + defaultUserSimilarity + ")." ) { 
        (v: String, c: Config) => c.copy(userSimilarity = v) },
      booleanOpt(None, "weighted", "<true/false>", "The Similarity score is weighted (default false).") { 
        (v: Boolean, c: Config) => c.copy(weighted = v) },
      doubleOpt(None, "minSimilarity", "<double>", "Minimal similarity required for neighbors. No minimum if this is not specified (default).") { 
        (v: Double, c: Config) => c.copy(minSimilarity = Some(v)) },
      doubleOpt(None, "samplingRate", "<double>", "Must be > 0 and <=1 (Default 1). Percentage of users to consider when building neighborhood." + 
        " Decrease to trade quality for performance.") { 
        (v: Double, c: Config) => c.copy(samplingRate = v)}
      )}

    val config = parser.parse(args, Config()) map { config => 

      println(config)
      logger.info("Runing GenericUserBased with parameters: " + config)

      val model: DataModel = new FileDataModel (new File(config.input))
      
      val weighted: Weighting = if (config.weighted) Weighting.WEIGHTED else Weighting.UNWEIGHTED

      val similarity: UserSimilarity = config.userSimilarity match {
        case "CityBlockSimilarity" => new CityBlockSimilarity(model)
        case "EuclideanDistanceSimilarity" => new EuclideanDistanceSimilarity(model, weighted)
        case "LogLikelihoodSimilarity" => new LogLikelihoodSimilarity(model)
        case "PearsonCorrelationSimilarity" => new PearsonCorrelationSimilarity(model, weighted)
        case "SpearmanCorrelationSimilarity" => new SpearmanCorrelationSimilarity(model)
        case "TanimotoCoefficientSimilarity" => new TanimotoCoefficientSimilarity(model)
        case "UncenteredCosineSimilarity" => new UncenteredCosineSimilarity(model, weighted)
        case _ => throw new RuntimeException("Invalid UserSimilarity")
      }

      val minSimilarity = config.minSimilarity.getOrElse(Double.NegativeInfinity)

      val neighborhood: UserNeighborhood = new NearestNUserNeighborhood(config.nearestN, minSimilarity, similarity, model, config.samplingRate)

      val recommender: Recommender = if (config.booleanData) {
        new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity)
      } else {
        new GenericUserBasedRecommender(model, neighborhood, similarity)
      }

      val userIds = model.getUserIDs

      val output = new FileWriter(config.output)
    
      val numRecommendations = config.numRecommendations

      while (userIds.hasNext) {
        val uid = userIds.next
        val rec = recommender.recommend(uid, numRecommendations)
        if (rec.size != 0) {
          val prediction = uid+"\t"+"[" + (rec map {x => x.getItemID +":"+x.getValue }).mkString(",") + "]"
          output.write(prediction+"\n")
        }
      }

      output.close()

    } getOrElse {
      println("Invalid arguments.")
    }

  }
}
