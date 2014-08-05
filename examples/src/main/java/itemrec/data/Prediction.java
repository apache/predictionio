package io.prediction.examples.java.itemrec.data;

import java.io.Serializable;
import java.util.List;

public class Prediction implements Serializable {
  public List<Integer> iids;
  public List<Float> scores;

  public Prediction(List<Integer> iids, List<Float> scores) {
    this.iids = iids;
    this.scores = scores;
  }

  @Override
  public String toString() {
    return "Prediction: " + iids.toString() + ";" + scores.toString();
  }
}
