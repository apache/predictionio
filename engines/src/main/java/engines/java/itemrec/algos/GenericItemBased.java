package io.prediction.engines.java.itemrec.algos;

import io.prediction.engines.java.itemrec.data.TrainingData;

import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.CityBlockSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.common.TasteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// use TrainigData as CD for now
public class GenericItemBased
  extends MahoutAlgorithm<GenericItemBasedParams> {

  final static Logger logger = LoggerFactory.getLogger(GenericItemBased.class);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public Recommender buildRecommender(TrainingData cd) throws TasteException {

    // TODO: read from params
    String itemSimilarity = "LogLikelihoodSimilarity";
    boolean weighted = false;

    Weighting weightedParam;

    if (weighted)
      weightedParam = Weighting.WEIGHTED;
    else
      weightedParam = Weighting.UNWEIGHTED;

    ItemSimilarity similarity;
    switch (itemSimilarity) {
      case "CityBlockSimilarity":
        similarity = new CityBlockSimilarity(cd.dataModel);
        break;
      case "EuclideanDistanceSimilarity":
        similarity = new EuclideanDistanceSimilarity(cd.dataModel, weightedParam);
        break;
      case "LogLikelihoodSimilarity":
        similarity = new LogLikelihoodSimilarity(cd.dataModel);
        break;
      case "PearsonCorrelationSimilarity":
        similarity = new PearsonCorrelationSimilarity(cd.dataModel, weightedParam);
        break;
      case "TanimotoCoefficientSimilarity":
        similarity = new TanimotoCoefficientSimilarity(cd.dataModel);
        break;
      case "UncenteredCosineSimilarity":
        similarity = new UncenteredCosineSimilarity(cd.dataModel, weightedParam);
        break;
      default:
        similarity = new LogLikelihoodSimilarity(cd.dataModel);
        break;
    }

    Recommender recommender = new GenericItemBasedRecommender(
      cd.dataModel,
      similarity
      // TODO: support other candidate item strategy
      //AbstractRecommender.getDefaultCandidateItemsStrategy(),
      //GenericItemBasedRecommender.getDefaultMostSimilarItemsCandidateItemsStrategy()
      );

    return recommender;
  }
}
