package myrecommendations;

import io.prediction.controller.java.LJavaAlgorithm;
import io.prediction.controller.EmptyParams;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;
import scala.Tuple2;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Algorithm extends
  LJavaAlgorithm<EmptyParams, TrainingData, Model, Query, Float> {

  @Override
  public Model train(TrainingData data) {
    // pre-process
    Map<Integer, Map<Integer, Float>> itemMap = new HashMap<Integer, Map<Integer, Float>>();
    Map<Integer, Integer> userIndexMap = new HashMap<Integer, Integer>();
    Map<Integer, Integer> itemIndexMap = new HashMap<Integer, Integer>();

    int itemIndex = 0;
    int userIndex = 0;
    for (TrainingData.Rating r : data.ratings) {
      Map<Integer, Float> userRating = itemMap.get(r.iid);
      if (userRating == null) {
        // new item
        userRating = new HashMap<Integer, Float>();
        itemMap.put(r.iid, userRating);
        itemIndexMap.put(r.iid, itemIndex);
        itemIndex += 1; // increment item index for next item
      }
      userRating.put(r.uid, r.rating);

      // update user index
      Integer u = userIndexMap.get(r.uid);
      if (u == null) {
        // new user
        userIndexMap.put(r.uid, userIndex);
        userIndex += 1;
      }
    }

    int numOfItems = itemIndexMap.size();
    int numOfUsers = userIndexMap.size();

    Map<Integer, RealVector> itemVectors = new HashMap<Integer, RealVector>();
    Map<Integer, RealVector> userHistory = new HashMap<Integer, RealVector>();

    for (Map.Entry<Integer, Map<Integer, Float>> entry : itemMap.entrySet()) {
      Integer itemID = entry.getKey();
      Integer iindex = itemIndexMap.get(itemID);
      Map<Integer, Float> userRatingMap = entry.getValue();
      RealVector item = new ArrayRealVector(numOfUsers); // dimension is numOfUsers
      for (Map.Entry<Integer, Float> r : userRatingMap.entrySet()) {
        Integer userID = r.getKey();
        Float rating = r.getValue();
        Integer uindex = userIndexMap.get(userID);
        item.setEntry(uindex, rating);
        // update user History
        RealVector user = userHistory.get(userID);
        if (user == null) {
          user = new ArrayRealVector(numOfItems);
          userHistory.put(userID, user);
        }
        user.setEntry(iindex, rating);
      }
      itemVectors.put(itemID, item);
    }

    // calculate sim
    Map<Integer, RealVector> itemSimilarity = new HashMap<Integer, RealVector>();
    List<Integer> item1List = new ArrayList<Integer>(itemIndexMap.keySet());
    List<Integer> item2List = new ArrayList<Integer>(item1List);

    for (Integer itemID1 : item1List) {
      item2List.remove(0);
      Integer index1 = itemIndexMap.get(itemID1);
      for (Integer itemID2: item2List) {
        RealVector vector1 = itemVectors.get(itemID1);
        RealVector vector2 = itemVectors.get(itemID2);
        double score = vector1.cosine(vector2);
        Integer index2 = itemIndexMap.get(itemID2);
        setItemSimilarity(itemSimilarity, itemID1, index2, score, numOfItems);
        setItemSimilarity(itemSimilarity, itemID2, index1, score, numOfItems);
      }
    }

    return new Model(itemSimilarity, userHistory);
  }

  private void setItemSimilarity(Map<Integer, RealVector> itemSimilarity,
    Integer itemID1, Integer index2, double score, int dimension) {
      RealVector vector = itemSimilarity.get(itemID1);
      if (vector == null) {
        vector = new ArrayRealVector(dimension);
        itemSimilarity.put(itemID1, vector);
      }
    vector.setEntry(index2, score);
  }

  @Override
  public Float predict(Model model, Query query) {
    return 0.0f;
  }
}
