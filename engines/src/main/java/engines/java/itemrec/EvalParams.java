package io.prediction.engines.java.itemrec;

import io.prediction.BaseParams;

public class EvalParams implements BaseParams {
  public String filePath; // file path
  public int goal; // rate >= goal
  public Boolean verbose;

  public EvalParams(String path, int goal, Boolean verbose) {
    this.filePath = path;
    this.goal = goal;
    this.verbose = verbose;
  }
}
