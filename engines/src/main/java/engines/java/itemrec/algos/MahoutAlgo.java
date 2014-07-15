package io.prediction.engines.java.itemrec.algos;

import io.prediction.engines.java.itemrec.data.TrainingData;
import io.prediction.engines.java.itemrec.data.Feature;
import io.prediction.engines.java.itemrec.data.Prediction;
import io.prediction.engines.java.itemrec.data.Model;
import io.prediction.java.JavaLocalAlgorithm;
import io.prediction.BaseParams;

import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.common.TasteException;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.slf4j.Logger;

public abstract class MahoutAlgo<AP extends BaseParams>
  extends JavaLocalAlgorithm<TrainingData, Feature, Prediction,
  Model, AP> {

  abstract public Logger getLogger();

  abstract public Recommender buildRecommender(TrainingData cd);

  @Override
  public Model train(TrainingData cd) {
    Logger logger = getLogger();
    Recommender recommender = buildRecommender(cd);
    Map<Long, List<RecommendedItem>> itemRecScores = new HashMap<Long, List<RecommendedItem>>();
    Iterator<Long> uids;

    try {
      uids = cd.dataModel.getUserIDs();
    } catch (TasteException e){
      logger.error("Caught TasteException " + e.getMessage());
      uids = (new ArrayList<Long>()).iterator();
    }

    while (uids.hasNext()) {
      Long uid = uids.next();
      List<RecommendedItem> items;
      try {
        // TOOD: get numRecommendation from params
        items = recommender.recommend(uid, 100);
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
  public Prediction predict(Model model, Feature feature) {
    Logger logger = getLogger();

    List<RecommendedItem> items = model.itemRecScores.get((long) feature.uid);

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
        it.hasNext() && (i < feature.n);
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
