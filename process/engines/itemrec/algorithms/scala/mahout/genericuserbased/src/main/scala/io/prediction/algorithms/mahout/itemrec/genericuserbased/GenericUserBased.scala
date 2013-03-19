package io.prediction.algorithms.mahout.itemrec.genericuserbased

//import java.util.List
import java.io.File
import java.io.FileWriter

import scala.collection.JavaConversions._

import grizzled.slf4j.Logger

import io.prediction.commons.filepath.{DataFile, AlgoFile}
import io.prediction.commons.Config

import scala.sys.process._

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

  case class Param(
    /*input: String = "ratings.csv",
    output: String = "predicted.tsv",*/
    hdfsRoot: String = "",
    localTempRoot: String = "",
    appid: Int = 0,
    engineid: Int = 0,
    algoid: Int = 0,
    evalid: Option[Int] = None,
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

    val parser = new scopt.immutable.OptionParser[Param]("GenericUserBased", "") { def options = Seq(
      /*opt(None, "input", "<File Name>", "User preference input file (default ratings.csv).") { 
        (v: String, c: Param) => c.copy(input = v) 
      },
      opt(None, "output", "<File Name>", "Recommendation output file (default predicted.tsv).") { 
        (v: String, c: Param) => c.copy(output = v) 
      },*/
      opt(None, "hdfsRoot", "<dir>", "HDFS root directory.") { 
        (v: String, c: Param) => c.copy(hdfsRoot = v) },
      opt(None, "localTempRoot", "<dir>", "local temporary directory") {
        (v: String, c: Param) => c.copy(localTempRoot = v) },
      intOpt(None, "appid", "<int>", "App Id.") { 
        (v: Int, c: Param) => c.copy(appid = v) },
      intOpt(None, "engineid", "<int>", "Engine Id.") {
        (v: Int, c: Param) => c.copy(engineid = v) },
      intOpt(None, "algoid", "<int>", "Algo Id.") {
        (v: Int, c: Param) => c.copy(algoid = v) },
      intOpt(None, "evalid", "<int>", "Offline Eval Id.") {
        (v: Int, c: Param) => c.copy(evalid = Some(v)) },
      booleanOpt(None, "booleanData", "<true/false>", "Treat input data as having no preference values (Default false).") { 
        (v: Boolean, c: Param) => c.copy(booleanData = v) },
      intOpt(None, "numRecommendations", "<int>", "Number of recommendations to compute per user (default 10).") { 
        (v: Int, c: Param) => c.copy(numRecommendations = v) }, 
      intOpt(None, "nearestN", "<int>", "Nearest n users to a given user (default 10).") { 
        (v: Int, c: Param) => c.copy(nearestN = v) },
      opt(None, "userSimilarity", "<User Similarity Measure>", "User Similarity Measures: " + 
        userSimilarityValues.mkString(", ") + ". (Default " + defaultUserSimilarity + ")." ) { 
        (v: String, c: Param) => c.copy(userSimilarity = v) },
      booleanOpt(None, "weighted", "<true/false>", "The Similarity score is weighted (default false).") { 
        (v: Boolean, c: Param) => c.copy(weighted = v) },
      doubleOpt(None, "minSimilarity", "<double>", "Minimal similarity required for neighbors. No minimum if this is not specified (default).") { 
        (v: Double, c: Param) => c.copy(minSimilarity = Some(v)) },
      doubleOpt(None, "samplingRate", "<double>", "Must be > 0 and <=1 (Default 1). Percentage of users to consider when building neighborhood." + 
        " Decrease to trade quality for performance.") { 
        (v: Double, c: Param) => c.copy(samplingRate = v)}
      )}

    val param = parser.parse(args, Param()) map { param => 

      logger.info("Runing GenericUserBased with args: " + param)

      val commonsConfig = new Config
      
      /** Try search path if hadoop home is not set. */
      val hadoopCommand = commonsConfig.settingsHadoopHome map { h => h+"/bin/hadoop" } getOrElse { "hadoop" }

      // input file
      val hdfsRatingsPath = DataFile(param.hdfsRoot, param.appid, param.engineid, param.algoid, param.evalid, "ratings.csv")

      val localRatingsPath = param.localTempRoot + "algo-" + param.algoid + "-ratings.csv"
      val localRatingsFile = new File(localRatingsPath)
      localRatingsFile.getParentFile().mkdirs() // create parent dir
      if (localRatingsFile.exists()) localRatingsFile.delete() // delete existing file first

      val copyFromHdfsRatingsCmd = s"$hadoopCommand fs -getmerge $hdfsRatingsPath $localRatingsPath"
      logger.info("Executing '%s'...".format(copyFromHdfsRatingsCmd))
      if ((copyFromHdfsRatingsCmd.!) != 0)
        throw new RuntimeException("Failed to execute '%s'".format(copyFromHdfsRatingsCmd))
      
      val model: DataModel = new FileDataModel(localRatingsFile)
      
      val weighted: Weighting = if (param.weighted) Weighting.WEIGHTED else Weighting.UNWEIGHTED

      val similarity: UserSimilarity = param.userSimilarity match {
        case "CityBlockSimilarity" => new CityBlockSimilarity(model)
        case "EuclideanDistanceSimilarity" => new EuclideanDistanceSimilarity(model, weighted)
        case "LogLikelihoodSimilarity" => new LogLikelihoodSimilarity(model)
        case "PearsonCorrelationSimilarity" => new PearsonCorrelationSimilarity(model, weighted)
        case "SpearmanCorrelationSimilarity" => new SpearmanCorrelationSimilarity(model)
        case "TanimotoCoefficientSimilarity" => new TanimotoCoefficientSimilarity(model)
        case "UncenteredCosineSimilarity" => new UncenteredCosineSimilarity(model, weighted)
        case _ => throw new RuntimeException("Invalid UserSimilarity: " + param.userSimilarity)
      }

      val minSimilarity = param.minSimilarity.getOrElse(Double.NegativeInfinity)

      val neighborhood: UserNeighborhood = new NearestNUserNeighborhood(param.nearestN, minSimilarity, similarity, model, param.samplingRate)

      val recommender: Recommender = if (param.booleanData) {
        new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity)
      } else {
        new GenericUserBasedRecommender(model, neighborhood, similarity)
      }

      
      // output file
      val localPredictedPath = param.localTempRoot + "algo-"+ param.algoid + "-predicted.tsv"
      val localPredictedFile = new File(localPredictedPath)

      localPredictedFile.getParentFile().mkdirs() // create parent dir
      if (localPredictedFile.exists()) localPredictedFile.delete() // delete existing file first

      val output = new FileWriter(localPredictedFile)
    
      val numRecommendations = param.numRecommendations

      val userIds = model.getUserIDs

      while (userIds.hasNext) {
        val uid = userIds.next
        val rec = recommender.recommend(uid, numRecommendations)
        if (rec.size != 0) {
          val prediction = uid+"\t"+"[" + (rec map {x => x.getItemID +":"+x.getValue }).mkString(",") + "]"
          output.write(prediction+"\n")
        }
      }

      output.close()

      // output file
      val hdfsPredictedPath = AlgoFile(param.hdfsRoot, param.appid, param.engineid, param.algoid, param.evalid, "predicted.tsv")

      // delete the hdfs file if it exists, otherwise copyFromLocal will fail.
      val deleteHdfsPredictedCmd = s"$hadoopCommand fs -rmr $hdfsPredictedPath"
      val copyToHdfsPredictedCmd = s"$hadoopCommand fs -copyFromLocal $localPredictedPath $hdfsPredictedPath"

      logger.info("Executing '%s'...".format(deleteHdfsPredictedCmd))
      deleteHdfsPredictedCmd.!
      
      logger.info("Executing '%s'...".format(copyToHdfsPredictedCmd))
      if ((copyToHdfsPredictedCmd.!) != 0)
        throw new RuntimeException("Failed to execute '%s'".format(copyToHdfsPredictedCmd))

      logger.info("Deleting temporary file " + localRatingsFile.getPath)
      localRatingsFile.delete()
      logger.info("Deleting temporary file " + localPredictedFile.getPath)
      localPredictedFile.delete()

    } getOrElse {
      println("Invalid arguments.")
    }

  }
}
