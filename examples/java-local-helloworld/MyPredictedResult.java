package org.sample.java.helloworld;

import java.io.Serializable;

public class MyPredictedResult implements Serializable {
  Double temperature;

  public MyPredictedResult(Double temperature) {
    this.temperature = temperature;
  }
}
