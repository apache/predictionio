package myengine;

import io.prediction.controller.IEngineFactory;
import io.prediction.controller.java.JavaEngine;
import io.prediction.controller.java.JavaEngineBuilder;

import myengine.MyParams.DataParams;
import myengine.MyData.TrainingData;
import myengine.MyData.PreparedData;
import myengine.MyData.Query;
import myengine.MyData.PredictedResult;
import myengine.MyData.Actual;

public class EngineFactory implements IEngineFactory {
  public JavaEngine<TrainingData, DataParams, PreparedData, Query, PredictedResult, Actual> apply() {
    return new JavaEngineBuilder<
      TrainingData, DataParams, PreparedData, Query, PredictedResult, Actual>()
      .dataSourceClass(DataSource.class)
      .preparatorClass(Preparator.class)
      .addAlgorithmClass("AlgorithmName", Algorithm.class)
      .servingClass(Serving.class)
      .build();
  }
}
