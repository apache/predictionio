package io.prediction.examples.java.parallel;

import io.prediction.controller.java.EmptyParams;
import io.prediction.controller.java.LJavaServing;

public class Serving extends LJavaServing<EmptyParams, Query, Float> {

  public Serving() {

  }

  @Override
  public Float serve(Query query, Iterable<Float> predictions) {
    return predictions.iterator().next();
  }

}
