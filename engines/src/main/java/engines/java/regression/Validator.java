package io.prediction.engines.java.regression;

import io.prediction.java.JavaValidator;
import java.lang.Iterable;
import io.prediction.EmptyParams;
import scala.Tuple3;
import java.util.List;
import java.util.ArrayList;


public class Validator
  extends JavaValidator<EmptyParams, EmptyParams, EmptyParams,
          Double[], Double, Double, Double, Double, String> {
  public Double validate(Double[] feature, Double prediction, Double actual) {
    return prediction - actual;
  }

  public Double validateSet(
      EmptyParams tdp, EmptyParams edp, Iterable<Double> vus) {
    double mse = 0.0;
    int n = 0;
    for (Double e: vus) {
      mse += e;
      n += 1;
    }
    return mse / n;
  }

  public String crossValidate(
      Iterable<Tuple3<EmptyParams, EmptyParams, Double>> input) {
    List<String> l = new ArrayList<String>();
    for (Tuple3<EmptyParams, EmptyParams, Double> t : input) {
      l.add("MSE: " + t._3().toString());
    }
    return l.toString();
  }
}


