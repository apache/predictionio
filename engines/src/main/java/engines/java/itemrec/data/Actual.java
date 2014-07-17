package io.prediction.engines.java.itemrec.data;

import java.io.Serializable;
import java.util.List;

public class Actual implements Serializable {
  public List<Integer> iids;

  public Actual(List<Integer> iids) {
    this.iids = iids;
  }
}
