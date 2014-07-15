package io.prediction.engines.java.itemrec.algos;

import io.prediction.engines.java.itemrec.data.TrainingData;

import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.Factorizer;
import org.apache.mahout.cf.taste.common.TasteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SVDPlusPlus
  extends MahoutAlgorithm<SVDPlusPlusParams> {

  final static Logger logger = LoggerFactory.getLogger(SVDPlusPlus.class);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public Recommender buildRecommender(TrainingData cd) {
    // TODO: read from params
    int numFeatures = 3;
    double learningRate = 0.01;
    double preventOverfitting = 0.1;
    double randomNoise = 0.01;
    int numIterations = 3;
    double learningRateDecay = 1;
    boolean unseenOnly = false;

    Recommender recommender = null;
    // TODO: handle Exception
    try {
    Factorizer factorizer = new SVDPlusPlusFactorizer(
      cd.dataModel, numFeatures, learningRate, preventOverfitting,
      randomNoise, numIterations, learningRateDecay);

    // TODO: support other candidate item strategy
      recommender = new SVDRecommender(cd.dataModel, factorizer);
    } catch (TasteException e) {
      logger.error("Caught TasteException " + e.getMessage());
    }
    return recommender;
  }
}
