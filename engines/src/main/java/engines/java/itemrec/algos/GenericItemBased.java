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
  extends JavaLocalAlgorithm<TrainingData, Feature, Prediction,
  Model, GenericItemBasedParams> {

  final static Logger logger = LoggerFactory.getLogger(GenericItemBased.class);

  @Override
  public Model train(TrainingData cd) {

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

    Map<Long, List<RecommendedItem>> itemRecScores = new HashMap<Long, List<RecommendedItem>>();

    Iterator<Long> uids;
    try {
      uids = cd.dataModel.getUserIDs();
    } catch (TasteException e){
      logger.error("Caught TasteException " + e.getMessage());
      uids = (new ArrayList<Long>()).iterator();
    }

    // TODO: refactor this with other recommmedner
    // TOOD: get numRecommendation from params
    while (uids.hasNext()) {
      Long uid = uids.next();
      List<RecommendedItem> items;
      try {
        items = recommender.recommend(uid, 100);
      } catch (TasteException e) {
        items = new ArrayList<RecommendedItem>();
        logger.error("Caught TasteException " + e.getMessage());
      }
      if (items.size() > 0) {
        itemRecScores.put(uid, items);
      }
    }

    return new Model(itemRecScores);
  }

  @Override
  public Prediction predict(Model model, Feature feature) {

    List<RecommendedItem> items = model.itemRecScores.get(feature.uid);

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
