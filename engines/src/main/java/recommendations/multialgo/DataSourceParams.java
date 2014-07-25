package io.prediction.engines.java.recommendations.multialgo;

import io.prediction.controller.Params;

public class DataSourceParams implements Params {
  //public String filePath; // file path
  public String dir;

  public DataSourceParams(String dir) {
    this.dir = dir;
  }
}
