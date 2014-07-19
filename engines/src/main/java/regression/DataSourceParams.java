package io.prediction.engines.java.regression;

import io.prediction.controller.Params;

public class DataSourceParams implements Params {
  public final String filepath;
  public DataSourceParams(String filepath) {
    this.filepath = filepath;
  }
}
