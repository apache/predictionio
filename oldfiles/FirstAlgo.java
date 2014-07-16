package io.prediction;

import io.prediction.engines.test.AlgoClass;

public class FirstAlgo extends AlgoClass<Model> {
  public int g(int e) {
    return e + 100;
  }

  public Model get(int e) {
    return new Model(e);
  }
}

