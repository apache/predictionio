package io.prediction.engines.java.regression;

import io.prediction.java.JavaLocalEngine;
import io.prediction.EngineFactory;
import java.util.Map;
import java.util.HashMap;
import io.prediction.java.JavaLocalAlgorithm;

// Doesn't work yet. Has type system failure.....
/*
public class Engine implements EngineFactory {
  public JavaLocalEngine<TrainingData, TrainingData, Double[], Double> apply() {
    Map<String, Class<? extends JavaLocalAlgorithm<TrainingData, Double[], Double, ?, ?>>> algoMap = 
      new HashMap <> ();

    algoMap.put("default", Algo.class);
    algoMap.put("dummy", DummyAlgo.class);

    return new JavaLocalEngine<TrainingData, TrainingData, Double[], Double>(Cleanser.class, algoMap, Server.class);
  }
}
*/



