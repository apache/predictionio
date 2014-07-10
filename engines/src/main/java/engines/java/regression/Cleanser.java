package io.prediction.engines.java.regression;

import io.prediction.java.JavaLocalCleanser;
import io.prediction.EmptyParams;

// This cleanser is just a proof-of-concept.
// It remove the second half of the input data
public class Cleanser
  extends JavaLocalCleanser<TrainingData, TrainingData, EmptyParams> {

  public TrainingData cleanse(TrainingData td) {
    int n = td.r / 2;

    Double[][] x = new Double[n][td.c];
    Double[] y = new Double[n];
    for (int i=0; i<n; i++) {
      for (int j=0; j<td.c; j++) {
        x[i][j] = td.x[i][j];
      }
      y[i] = td.y[i];
    }

    return new TrainingData(x, y);
  }
}
  

