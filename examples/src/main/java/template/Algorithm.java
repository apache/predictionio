package myengine;

import io.prediction.controller.java.LJavaAlgorithm;

import myengine.MyParams.AlgoParams;
import myengine.MyData.PreparedData;
import myengine.MyData.Model;
import myengine.MyData.Query;
import myengine.MyData.PredictedResult;

public class Algorithm extends
  LJavaAlgorithm<AlgoParams, PreparedData, Model, Query, PredictedResult> {

  public Algorithm(AlgoParams params) {

  }

  @Override
  public Model train(PreparedData data) {
    return new MyData.Model();
  }

  @Override
  public PredictedResult predict(Model model, Query query) {
    return new PredictedResult();
  }
}
