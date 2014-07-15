package io.prediction.engines.java.itemrec.algos;

import io.prediction.engines.java.itemrec.data.TrainingData;
import io.prediction.engines.java.itemrec.data.Feature;
import io.prediction.engines.java.itemrec.data.Prediction;
import io.prediction.engines.java.itemrec.data.Model;
import io.prediction.java.JavaLocalAlgorithm;

import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.common.TasteException;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// use TrainigData as CD for now
public class GenericItemBased
  extends MahoutAlgo<GenericItemBasedParams> {

  final static Logger logger = LoggerFactory.getLogger(GenericItemBased.class);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public Recommender buildRecommender(TrainingData cd) {
    // TODO: support other similarity measure
    ItemSimilarity similarity = new LogLikelihoodSimilarity(cd.dataModel);

    // TODO: support other candidate item strategy
    Recommender recommender = new GenericItemBasedRecommender(
      cd.dataModel,
      similarity
      /*
      AbstractRecommender.getDefaultCandidateItemsStrategy(),
      GenericItemBasedRecommender.getDefaultMostSimilarItemsCandidateItemsStrategy()*/
      );

    return recommender;
  }
}
