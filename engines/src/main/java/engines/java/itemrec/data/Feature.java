package io.prediction.engines.java.itemrec.data;

import java.io.Serializable;

public class Feature implements Serializable {
  public int uid;
  public int n;

  public Feature(int uid, int n) {
    this.uid = uid;
    this.n = n;
  }

  @Override
  public String toString() {
    return uid + " " + n;
  }
}
