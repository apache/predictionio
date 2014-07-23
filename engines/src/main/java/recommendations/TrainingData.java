package myrecommendations;

import java.io.Serializable;
import java.util.List;

public class TrainingData implements Serializable {
  public List<Rating> ratings;

  public TrainingData(List<Rating> ratings) {
    this.ratings = ratings;
  }

  public static class Rating {
    public int uid; // user ID
    public int iid; // item ID
    public float rating;

    public Rating(int uid, int iid, float rating) {
      this.uid = uid;
      this.iid = iid;
      this.rating = rating;
    }
  }
}
