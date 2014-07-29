package io.prediction.engines.java.recommendations.multialgo;

import io.prediction.controller.Params;

public class DataSourceParams implements Params {
  public String dir;
  public boolean addFakeData;

  public DataSourceParams(String dir, boolean addFakeData) {
    this.dir = dir;
    this.addFakeData = addFakeData;
  }

  public DataSourceParams(String dir) {
    this(dir, false);
  }
}
