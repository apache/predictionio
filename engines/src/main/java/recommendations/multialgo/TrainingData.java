package io.prediction.engines.java.recommendations.multialgo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class TrainingData implements Serializable {
  public List<Rating> ratings;
  public List<String> genres;
  public Map<Integer, String[]> itemInfo;

  public TrainingData(List<Rating> ratings, List<String> genres, Map<Integer, String[]> itemInfo) {
    this.ratings = ratings;
    this.genres = genres;
    this.itemInfo = itemInfo;
  }

  @Override
  public String toString() {
    return "TrainingData: rating.size=" + ratings.size() + " genres.size=" + genres.size()
      + " itemInfo.size=" + itemInfo.size();
    //return "TrainingData: " + ratings.toString() + "\nGenres: " + genres.toString() 
    //  + "\nItemInfo: " + itemInfo.toString();
  }

  public static class Rating implements Serializable {
    public int uid; // user ID
    public int iid; // item ID
    public float rating;

    public Rating(int uid, int iid, float rating) {
      this.uid = uid;
      this.iid = iid;
      this.rating = rating;
    }

    @Override
    public String toString() {
      return "(" + uid + "," + iid + "," + rating + ")";
    }
  }
}
