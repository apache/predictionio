package io.prediction.engines.java.itemrec.algos;

import io.prediction.engines.java.itemrec.data.TrainingData;
import io.prediction.engines.java.itemrec.data.Query;
import io.prediction.engines.java.itemrec.data.Prediction;
import io.prediction.engines.java.itemrec.data.Model;
import io.prediction.controller.java.LJavaAlgorithm;
import io.prediction.controller.Params;

import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.common.TasteException;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.slf4j.Logger;

/** Mahout Legacy single machine algorithm */
public abstract class MahoutAlgorithm<AP extends Params>
  extends LJavaAlgorithm<AP, TrainingData, Model, Query, Prediction> {

  Logger logger;
  MahoutParams params;

  public MahoutAlgorithm(MahoutParams params, Logger logger) {
    this.logger = logger;
    this.params = params;
  }

  abstract public Recommender buildRecommender(TrainingData data) throws TasteException;

  @Override
  public Model train(TrainingData data) {
    Recommender recommender = null;

    try {
      recommender = buildRecommender(data);
    } catch (TasteException e) {
      logger.error("Caught TasteException " + e.getMessage());
    }

    Map<Long, List<RecommendedItem>> itemRecScores = new HashMap<Long, List<RecommendedItem>>();
    Iterator<Long> uids;

    try {
      uids = data.dataModel.getUserIDs();
    } catch (TasteException e){
      logger.error("Caught TasteException " + e.getMessage());
      uids = (new ArrayList<Long>()).iterator();
    }

    while (uids.hasNext()) {
      Long uid = uids.next();
      List<RecommendedItem> items;
      try {
        items = recommender.recommend(uid, params.numRecommendations);
      } catch (TasteException e) {
        items = new ArrayList<RecommendedItem>();
        logger.error("Caught TasteException " + e.getMessage());
      }
      if (items.size() > 0) {
        itemRecScores.put(uid, items);
      }
    }

    Model m = new Model(itemRecScores);
    logger.info(m.toString());
    return m;

  }

  @Override
  public Prediction predict(Model model, Query query) {
    List<RecommendedItem> items = model.itemRecScores.get((long) query.uid);

    logger.info(model.itemRecScores.keySet().toString());
    if (items != null) {
      logger.info(items.toString());
    } else {
      logger.info("null");
    }

    List<Integer> iids = new ArrayList<Integer>();
    List<Float> scores = new ArrayList<Float>();

    if (items != null) {

      int i = 0;
      for (Iterator<RecommendedItem> it = items.iterator();
        it.hasNext() && (i < query.n);
        i++) {
          RecommendedItem item = it.next();
          // TODO: check long to int casting
          iids.add((int) item.getItemID());
          scores.add(item.getValue());
        }
    }

    return new Prediction(iids, scores);
  }

}
