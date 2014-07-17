package io.prediction.api.java;

import io.prediction.api.EmptyParams;
import io.prediction.api.Params;
import java.util.ArrayList;
import java.util.List;
import scala.Tuple2;

public class JavaEngineParamsBuilder {
  private Params dataSourceParams = new EmptyParams();
  private Params preparatorParams = new EmptyParams();
  private List<Tuple2<String, Params>> algorithmParamsList = 
    new ArrayList<Tuple2<String, Params>>();  
  private Params servingParams = new EmptyParams();

  public JavaEngineParamsBuilder() {}

  public JavaEngineParamsBuilder dataSourceParams(Params p) {
    dataSourceParams = p;
    return this;
  }

  public JavaEngineParamsBuilder preparatorParams(Params p) {
    preparatorParams = p;
    return this;
  }

  public JavaEngineParamsBuilder addAlgorithmParams(String algorithmName, Params p) {
    algorithmParamsList.add(new Tuple2<String, Params>(algorithmName, p));
    return this;
  }
  
  public JavaEngineParamsBuilder servingParams(Params p) {
    servingParams = p;
    return this;
  }

  public JavaEngineParams build() {
    return new JavaEngineParams(
        dataSourceParams, preparatorParams, algorithmParamsList, servingParams);
  }
}

