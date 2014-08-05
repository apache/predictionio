package io.prediction.examples.java.itemrec.data;

import java.io.Serializable;

public class Rating implements Serializable {
  public int uid;
  public int iid;
  public float rating;
  public long t;

  public Rating(int uid, int iid, float rating, long t) {
    this.uid = uid;
    this.iid = iid;
    this.rating = rating;
    this.t = t;
  }
}
