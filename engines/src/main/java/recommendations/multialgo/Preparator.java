package io.prediction.engines.java.recommendations.multialgo;

import io.prediction.controller.java.LJavaPreparator;
import io.prediction.controller.EmptyParams;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Preparator extends LJavaPreparator<EmptyParams, TrainingData, PreparedData> {

  final static Logger logger = LoggerFactory.getLogger(DataSource.class);
  final int indexOffset = 5;

  public Preparator() {}

  public PreparedData prepare(TrainingData trainingData) {
    Map<Integer, Vector<Integer>> itemFeatures = new HashMap<Integer, Vector<Integer>>();

    for (Integer iid: trainingData.itemInfo.keySet()) {
      String[] info = trainingData.itemInfo.get(iid);

      Vector<Integer> features = new Vector<Integer>();
      for (int i=indexOffset; i < indexOffset + trainingData.genres.size(); i++) {
        features.add(Integer.parseInt(info[i]));
      }
      itemFeatures.put(iid, features);
    }

    return new PreparedData(trainingData, itemFeatures);
  }
}
