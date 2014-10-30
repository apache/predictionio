package myengine;

import io.prediction.controller.java.LJavaServing;

import myengine.MyParams.ServingParams;
import myengine.MyData.Query;
import myengine.MyData.PredictedResult;

public class Serving
  extends LJavaServing<ServingParams, Query, PredictedResult> {

  public Serving(ServingParams params) {

  }

  @Override
  public PredictedResult serve(Query query, Iterable<PredictedResult> predictions) {
    return new PredictedResult();
  }

}
