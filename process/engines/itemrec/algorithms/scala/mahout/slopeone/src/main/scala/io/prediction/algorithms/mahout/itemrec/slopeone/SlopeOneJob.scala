package io.prediction.algorithms.mahout.itemrec.slopeone

import scala.collection.JavaConversions._

import io.prediction.commons.mahout.itemrec.MahoutJob

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.common.Weighting
import org.apache.mahout.cf.taste.impl.recommender.slopeone.SlopeOneRecommender
import org.apache.mahout.cf.taste.impl.recommender.slopeone.MemoryDiffStorage

class SlopeOneJob extends MahoutJob {

  override def buildRecommender(dataModel: DataModel, args: Map[String, String]): Recommender = {

    // Weighting param:
    // - No_Weighting:
    // - Count: 
    // - Standard_Deviation: Weights preference difference with lower standard deviation more highly.
    val weightingArg: String = getArgOpt(args, "weighting", "Standard_Deviation") // weighted slope one recommender

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

    recommender
  }

}