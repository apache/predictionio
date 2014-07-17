package io.prediction.engines.java.regression;

import io.prediction.api.EmptyParams;
import io.prediction.api.java.LJavaAlgorithm;
import io.prediction.core.BaseAlgorithm2;
import io.prediction.api.Params;
import io.prediction.workflow.JavaAPIDebugWorkflow;
import io.prediction.workflow.APIDebugWorkflow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import scala.Tuple2;
import io.prediction.api.java.JavaEngineParams;


public class Run {
  public static void runComponents () {
    DataSourceParams dsp = new DataSourceParams("data/lr_data.txt");
    PreparatorParams pp = new PreparatorParams(0.3);
    
    List<Tuple2<String, Params>> algoParamsList = 
      new ArrayList<Tuple2<String, Params>>();
    algoParamsList.add(new Tuple2<String, Params>("OLS", new EmptyParams()));
    algoParamsList.add(new Tuple2<String, Params>("Default", new DefaultAlgorithmParams(0.2)));
    algoParamsList.add(new Tuple2<String, Params>("Default", new DefaultAlgorithmParams(0.4)));
    
    Map<String, 
      Class<? extends 
        LJavaAlgorithm<? extends Params, TrainingData, ?, Double[], Double>>> algoClassMap = 
      new HashMap <> ();
    
    algoClassMap.put("OLS", OLSAlgorithm.class);
    algoClassMap.put("Default", DefaultAlgorithm.class);
    
    JavaAPIDebugWorkflow.run(
        "Native Java",
        3,  // verbose
        DataSource.class,
        dsp,
        Preparator.class,
        pp,
        algoClassMap,
        algoParamsList,
        Serving.class,
        new EmptyParams(),
        MeanSquareMetrics.class,
        new EmptyParams()
        );
  }

  public static void runEngine() {
    
    List<Tuple2<String, Params>> algoParamsList = 
      new ArrayList<Tuple2<String, Params>>();
    algoParamsList.add(new Tuple2<String, Params>("OLS", new EmptyParams()));
    algoParamsList.add(new Tuple2<String, Params>("Default", new DefaultAlgorithmParams(0.2)));
    algoParamsList.add(new Tuple2<String, Params>("Default", new DefaultAlgorithmParams(0.4)));

    JavaEngineParams engineParams = new JavaEngineParams(
        new DataSourceParams("data/lr_data.txt"),
        new PreparatorParams(0.3),
        algoParamsList,
        new EmptyParams()
        );
     
    JavaAPIDebugWorkflow.runEngine(
        "java regression engine",
        3,  // verbose
        (new EngineFactory()).apply(),
        engineParams,
        MeanSquareMetrics.class,
        new EmptyParams()
        );
  }

  public static void main(String[] args) {
    runEngine();
  }
}
