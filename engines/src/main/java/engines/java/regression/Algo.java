package io.prediction.engines.java.regression;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.math3.linear.RealVector;
import java.util.Arrays;
import io.prediction.EmptyParams;

import io.prediction.java.JavaLocalAlgorithm;

public class Algo 
  extends JavaLocalAlgorithm<TrainingData, Double[], Double, Double[], 
          EmptyParams> {

  public Double[] train(TrainingData data) {
    OLSMultipleLinearRegression r = new OLSMultipleLinearRegression();
    // Convert Double[][] to double[][]
    double[][] x = new double[data.r][data.c];
    double[] y = new double[data.r];

    for (int i=0; i<data.r; i++) {
      for (int j=0; j<data.c; j++) {
        x[i][j] = data.x[i][j].doubleValue();
      }
      y[i] = data.y[i].doubleValue();
    }

    r.newSampleData(y, x);
    // Fixme. Add intercept
    double[] p = r.estimateRegressionParameters();
    Double[] pp = new Double[data.c];
    for (int j=0; j<data.c; j++) {
      pp[j] = p[j];
    }
    System.out.println("Regression Algo: " + Arrays.toString(pp));
    return pp;
  }

  public Double predict(Double[] model, Double[] query) {
    double dotProduct = 0.0;
    for (int i = 0; i < model.length; i++) {
      dotProduct += model[i] * query[i];
    }
    return dotProduct;
  }

  public static void main(String[] args) {
    OLSMultipleLinearRegression r = new OLSMultipleLinearRegression();
    double[] y = new double[]{11.0, 12.0, 13.0, 14.0, 15.0, 16.0};
    double[][] x = new double[6][];
    x[0] = new double[]{0, 0, 0, 0, 0};
    x[1] = new double[]{2.0, 0, 0, 0, 0};
    x[2] = new double[]{0, 3.0, 0, 0, 0};
    x[3] = new double[]{0, 0, 4.0, 0, 0};
    x[4] = new double[]{0, 0, 0, 5.0, 0};
    x[5] = new double[]{0, 0, 0, 0, 6.0};          
    r.newSampleData(y, x);

    double []p = r.estimateRegressionParameters();


    System.out.println(Arrays.toString(p));
  }
}
