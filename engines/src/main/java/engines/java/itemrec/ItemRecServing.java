package io.prediction.engines.java.itemrec;

import io.prediction.api.java.LJavaServing;
import io.prediction.engines.java.itemrec.data.Query;
import io.prediction.engines.java.itemrec.data.Prediction;

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
