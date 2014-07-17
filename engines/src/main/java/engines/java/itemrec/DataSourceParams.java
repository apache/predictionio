package io.prediction.engines.java.itemrec;

import io.prediction.controller.Params;

public class DataSourceParams implements Params {
  public String filePath; // file path
  public int goal; // rate >= goal

  public DataSourceParams(String path, int goal) {
    this.filePath = path;
    this.goal = goal;
  }
}
