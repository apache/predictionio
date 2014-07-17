package io.prediction.engines.java.itemrec;

import io.prediction.java.JavaServer;
import io.prediction.engines.java.itemrec.data.Feature;
import io.prediction.engines.java.itemrec.data.Prediction;

public class ItemRecServer extends JavaServer<Feature, Prediction, ServerParams> {
  @Override
  public Prediction combine(Feature feature, Iterable<Prediction> predictions) {
    // TODO: support combine multiple algo output
    return predictions.iterator().next();
  }
}
