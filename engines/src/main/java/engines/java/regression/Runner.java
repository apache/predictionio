package io.prediction.engines.java.regression;

import io.prediction.BaseParams;
import io.prediction.EmptyParams;
import scala.Tuple2;
import io.prediction.workflow.JavaDebugWorkflow;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import io.prediction.java.JavaLocalAlgorithm;

public class Runner {
  public static void main(String[] args) {
    DataParams edp = new DataParams("data/lr_data.txt");
    CleanserParams cp = new CleanserParams(0.8);
    EmptyParams ep = new EmptyParams();

    List<Tuple2<String, BaseParams>> algoParamsList = 
      new ArrayList<Tuple2<String, BaseParams>>();
    algoParamsList.add(new Tuple2<String,BaseParams>("default", ep));
    algoParamsList.add(new Tuple2<String,BaseParams>("dummy", ep));
    algoParamsList.add(new Tuple2<String,BaseParams>("dummy", ep));

    
    Map<String, 
      Class<? extends JavaLocalAlgorithm<TrainingData, Double[], Double, ?, ? extends BaseParams>>> algoClassMap = 
      new HashMap <> ();

    algoClassMap.put("default", Algo.class);
    algoClassMap.put("dummy", DummyAlgo.class);

    JavaDebugWorkflow.run(
        "Native Java",
        DataPreparator.class,
        Cleanser.class,
        algoClassMap,
        Server.class,
        Validator.class,
        edp,
        cp,
        algoParamsList,
        ep,
        ep
        );
  }
}
