package io.prediction.algorithms.mahout.itemrec.slopeone

import java.io.File
import java.io.FileWriter

import scala.collection.JavaConversions._

import io.prediction.commons.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel

import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.common.Weighting

import org.apache.mahout.cf.taste.impl.recommender.slopeone.SlopeOneRecommender
import org.apache.mahout.cf.taste.impl.recommender.slopeone.MemoryDiffStorage

class SlopeOneJob extends MahoutJob {

  override def run(args: Map[String, String]) = {
    val input = args("input")
    val output = args("output")
    val numRecommendations: Int = getArgOpt(args, "numRecommendations", "10").toInt
    // Weighting param:
    // - No_Weighting:
    // - Count: 
    // - Standard_Deviation: Weights preference difference with lower standard deviation more highly.
    val weightingArg: String = getArgOpt(args, "weighting", "Standard_Deviation") // weighted slope one recommender

    val dataModel: DataModel = new FileDataModel(new File(input))

    val (weighting, stdDevWeighting): (Weighting, Weighting) = weightingArg match {
      case "No_Weighting" => (Weighting.UNWEIGHTED, Weighting.UNWEIGHTED)
      case "Count" => (Weighting.WEIGHTED, Weighting.UNWEIGHTED)
      case "Standard_Deviation" => (Weighting.WEIGHTED, Weighting.WEIGHTED)
      case _ => throw new RuntimeException("Invalid weighting parameter: " + weightingArg)
    }

    val recommender: Recommender = new SlopeOneRecommender(dataModel,
         weighting, // weighting
         stdDevWeighting, // stdDevWeighting
         new MemoryDiffStorage(dataModel, stdDevWeighting, Long.MaxValue)); //maximum number of item-item average preference differences to track internally

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