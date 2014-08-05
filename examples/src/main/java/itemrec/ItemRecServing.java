package io.prediction.examples.java.itemrec;

import io.prediction.controller.java.LJavaServing;
import io.prediction.examples.java.itemrec.data.Query;
import io.prediction.examples.java.itemrec.data.Prediction;

public class ItemRecServing extends LJavaServing<ServingParams, Query, Prediction> {

  ServingParams params;

  public ItemRecServing(ServingParams params) {
    this.params = params;
  }

  @Override
  public Prediction serve(Query query, Iterable<Prediction> predictions) {
    // TODO: support combine multiple algo output
    return predictions.iterator().next();
  }
}
