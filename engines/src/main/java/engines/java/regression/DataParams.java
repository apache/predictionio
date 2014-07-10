package io.prediction.engines.java.regression;

import io.prediction.BaseParams;

public class DataParams implements BaseParams {
  public final String filepath;
  public DataParams(String filepath) {
    this.filepath = filepath;
  }
}
