package io.prediction.engines.java.recommendations.multialgo;

import java.util.Map;
import java.util.Vector;

public class PreparedData extends TrainingData {
  public final Map<Integer, Vector<Integer>> itemFeatures;

  public PreparedData(TrainingData data, Map<Integer, Vector<Integer>> itemFeatures) {
    super(data);
    this.itemFeatures = itemFeatures;
  }

  @Override
  public String toString() {
    return super.toString() + " itemFeatures.size" + itemFeatures.size();
  }
}
