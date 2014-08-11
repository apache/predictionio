package io.prediction.examples.java.itemrec;

import io.prediction.examples.java.itemrec.data.TrainingData;
import io.prediction.examples.java.itemrec.data.PreparedData;
import io.prediction.examples.java.itemrec.data.Query;
import io.prediction.examples.java.itemrec.data.Prediction;
import io.prediction.examples.java.itemrec.data.Actual;
import io.prediction.examples.java.itemrec.algos.GenericItemBased;
import io.prediction.examples.java.itemrec.algos.SVDPlusPlus;

import io.prediction.controller.java.IJavaEngineFactory;
import io.prediction.controller.java.EmptyParams;
import io.prediction.controller.java.JavaEngine;
import io.prediction.controller.java.JavaEngineBuilder;

public class EngineFactory implements IJavaEngineFactory {
  public JavaEngine<TrainingData, EmptyParams, PreparedData, Query, Prediction, Actual> apply() {
    return new JavaEngineBuilder<
      TrainingData, EmptyParams, PreparedData, Query, Prediction, Actual>()
      .dataSourceClass(ItemRecDataSource.class)
      .preparatorClass(ItemRecPreparator.class)
      .addAlgorithmClass("genericitembased", GenericItemBased.class)
      .addAlgorithmClass("svdplusplus", SVDPlusPlus.class)
      .servingClass(ItemRecServing.class)
      .build();
  }
}
