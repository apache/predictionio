package io.prediction.engines.java.itemrec.algos;

import io.prediction.engines.java.itemrec.data.PreparedData;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/** Mahout Legacy single machine algorithm */
public abstract class AbstractMahoutAlgorithm<AP extends Params>
  extends LJavaAlgorithm<AP, PreparedData, Model, Query, Prediction> {

  MahoutParams params;
  Logger logger;

  public AbstractMahoutAlgorithm(MahoutParams params, Logger logger) {
    this.params = params;
    this.logger = logger;
  }

  abstract public Recommender buildRecommender(PreparedData data) throws TasteException;

  @Override
  public Model train(PreparedData data) {
    Recommender recommender = null;

    try {
      recommender = buildRecommender(data);
    } catch (TasteException e) {
      logger.error("Caught TasteException " + e.getMessage());
    }

    List<Long> uids = new ArrayList<Long>();
    try {
      Iterator<Long> it = data.dataModel.getUserIDs();
      while (it.hasNext()) {
        uids.add(it.next());
      }
    } catch (TasteException e){
      logger.error("Caught TasteException " + e.getMessage());
    }

    //Map<Long, List<RecommendedItem>> itemRecScores = batchTrain(uids, recommender);
    Map<Long, List<RecommendedItem>> itemRecScores = batchTrainMulti(uids, recommender);

    Model m = new Model(itemRecScores);
    logger.info(m.toString());
    return m;

  }

  @Override
  public Prediction predict(Model model, Query query) {
    List<RecommendedItem> items = model.itemRecScores.get((long) query.uid);

    //logger.info(model.itemRecScores.keySet().toString());
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

  private Map<Long, List<RecommendedItem>> batchTrain(List<Long> uids, Recommender recommender) {
    Map<Long, List<RecommendedItem>> itemRecScores = new HashMap<Long, List<RecommendedItem>>();

    for (Long uid : uids) {
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

    return itemRecScores;
  }

  // multi threaded batch training
  private Map<Long, List<RecommendedItem>> batchTrainMulti(
    List<Long> uids, Recommender recommender) {

    int numProcessors = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(numProcessors);

    // TODO: configure batchSize
    List<List<Long>> batches = createBatches(uids, 50);
    List<Future<Map<Long, List<RecommendedItem>>>> results =
      new ArrayList<Future<Map<Long, List<RecommendedItem>>>>();

    for (List<Long> batch : batches) {
      RecommendTask task = new RecommendTask(batch, recommender);
      results.add(executor.submit(task));
    }

    // reduce stage
    Map<Long, List<RecommendedItem>> itemRecScores = new HashMap<Long, List<RecommendedItem>>();
    for (Future<Map<Long, List<RecommendedItem>>> r : results) {
      try {
        itemRecScores.putAll(r.get());
      } catch (Exception e) {
        logger.error("Caught Exception: " + e.getMessage());
      }
    }

    executor.shutdown();
    try {
      executor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.error("Caught Exception: " + e.getMessage());
    }
    return itemRecScores;
  }

  private List<List<Long>> createBatches(List<Long> data, int batchSize) {
    List<List<Long>> batches = new ArrayList<List<Long>>();

    int i = 0;
    boolean newBatch = true;
    int remain = data.size();
    List<Long> batchData = new ArrayList<Long>();
    int actual = 0;
    for (long d : data) {
      if (newBatch) {
        actual = Math.min(remain, batchSize);
        newBatch = false;
      }
      batchData.add(d);
      remain--;
      i++;
      if (i == actual) { // already fill up one batch
        batches.add(batchData);
        i = 0;
        newBatch = true;
        batchData = new ArrayList<Long>();
      }
    }
    logger.info("batches size: " + batches.size());
    return batches;
  }

  private class RecommendTask implements Callable<Map<Long, List<RecommendedItem>>> {

    private final List<Long> uids;
    private final Recommender recommender;

    public RecommendTask(List<Long> uids, Recommender recommender) {
      this.uids = uids;
      this.recommender = recommender;
    }

    @Override
    public Map<Long, List<RecommendedItem>> call() {
      return batchTrain(uids, recommender);
    }
  }

}
