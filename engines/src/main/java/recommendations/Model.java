package myrecommendations;

import java.io.Serializable;
import java.util.Map;
import org.apache.commons.math3.linear.RealVector;

public class Model implements Serializable {
  public Map<Integer, RealVector> itemSimilarity;
  public Map<Integer, RealVector> userHistory;

  public Model(Map<Integer, RealVector> itemSimilarity,
    Map<Integer, RealVector> userHistory) {
    this.itemSimilarity = itemSimilarity;
    this.userHistory = userHistory;
  }

  @Override
  public String toString() {
    return "Model: [itemSimilarity: " + itemSimilarity.toString() + "]\n"
      +"[userHistory: " + userHistory.toString() + "]";
  }
}
