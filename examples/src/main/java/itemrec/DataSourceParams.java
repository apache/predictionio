package io.prediction.examples.java.itemrec;

import io.prediction.controller.Params;

public class DataSourceParams implements Params {
  public String filePath; // file path
  public int iterations; // number of interations
  public float trainingPercentage;
  public float testPercentage;
  public int seed; // random split seed
  public int goal; // rate >= goal
  public int k; // for validation query only, should match metric's k if use MAP@k

  public DataSourceParams(String path, int iterations, float trainingPercentage,
    float testPercentage, int seed, int goal, int k) {
    this.filePath = path;
    this.iterations = iterations;
    // TODO: check percentage between 0 and 1.0
    this.trainingPercentage = trainingPercentage;
    this.testPercentage = testPercentage;
    this.seed = seed;
    this.goal = goal;
    this.k = k;
  }

  /** Default, all data is used for training and 1 iteration */
  public DataSourceParams(String path) {
    this(path, 1, 1.0f, 0.0f, 0, 0, 0);
  }
}
