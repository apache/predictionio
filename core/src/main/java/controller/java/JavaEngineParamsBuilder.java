package io.prediction.controller.java;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.Params;

import java.util.ArrayList;
import java.util.List;

import scala.Tuple2;

/**
 * Convenient builder class for assembling different types of parameters into a
 * single container of EngineParams.
 */
public class JavaEngineParamsBuilder {
  private Params dataSourceParams = new EmptyParams();
  private Params preparatorParams = new EmptyParams();
  private List<Tuple2<String, Params>> algorithmParamsList =
    new ArrayList<Tuple2<String, Params>>();
  private Params servingParams = new EmptyParams();

  /**
   * Instantiates an empty EngineParams builder.
   */
  public JavaEngineParamsBuilder() {}

  /**
   * Set the Data Source parameters of the Engine.
   */
  public JavaEngineParamsBuilder dataSourceParams(Params p) {
    dataSourceParams = p;
    return this;
  }

  /**
   * Set the Preparator parameters of the Engine.
   */
  public JavaEngineParamsBuilder preparatorParams(Params p) {
    preparatorParams = p;
    return this;
  }

  /**
   * Add an Algorithm parameter to the Engine.
   */
  public JavaEngineParamsBuilder addAlgorithmParams(String algorithmName, Params p) {
    algorithmParamsList.add(new Tuple2<String, Params>(algorithmName, p));
    return this;
  }

  /**
   * Set the Serving parameters of the Engine.
   */
  public JavaEngineParamsBuilder servingParams(Params p) {
    servingParams = p;
    return this;
  }

  /**
   * Build and return an instance of EngineParams.
   */
  public JavaEngineParams build() {
    return new JavaEngineParams(
        dataSourceParams, preparatorParams, algorithmParamsList, servingParams);
  }
}
