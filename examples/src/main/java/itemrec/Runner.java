package io.prediction.examples.java.itemrec;

import io.prediction.examples.java.itemrec.data.Query;
import io.prediction.examples.java.itemrec.data.Actual;
import io.prediction.examples.java.itemrec.data.Prediction;
import io.prediction.examples.java.itemrec.data.TrainingData;
import io.prediction.examples.java.itemrec.data.PreparedData;
import io.prediction.examples.java.itemrec.algos.GenericItemBased;
import io.prediction.examples.java.itemrec.algos.GenericItemBasedParams;
import io.prediction.examples.java.itemrec.algos.SVDPlusPlus;
import io.prediction.examples.java.itemrec.algos.SVDPlusPlusParams;
import io.prediction.controller.java.JavaParams;
import io.prediction.controller.java.EmptyParams;
import io.prediction.controller.java.LJavaAlgorithm;
import io.prediction.controller.java.JavaEngine;
import io.prediction.controller.java.JavaEngineBuilder;
import io.prediction.controller.java.JavaEngineParams;
import io.prediction.controller.java.JavaEngineParamsBuilder;
import io.prediction.controller.java.JavaWorkflow;
import io.prediction.controller.java.WorkflowParamsBuilder;

import scala.Tuple2;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class Runner {
  public static void main(String[] args) {

    String filePath = "src/main/java/itemrec/examples/ratings.csv";
    String algoName = "genericitembased";

    if (args.length == 2) {
      filePath = args[0];
      algoName = args[1];
    }

    System.out.println(Arrays.toString(args));
    int k = 20;
    DataSourceParams dsp = new DataSourceParams(filePath, 1, 0.8f, 0.2f, 0, 3, k);
    EmptyParams pp = new EmptyParams();
    GenericItemBasedParams genericItemBasedParams = new GenericItemBasedParams(10);
    SVDPlusPlusParams svdPlusPlusParams = new SVDPlusPlusParams(10);
    ServingParams sp = new ServingParams();

    String algo;
    JavaParams algoParams;
    if (algoName.equals("genericitembased")) {
      algo = "genericitembased";
      algoParams = genericItemBasedParams;
    } else {
      algo = "svdplusplus";
      algoParams = svdPlusPlusParams;
    }
/*
    List<Tuple2<String, JavaParams>> algoParamsList = new ArrayList<Tuple2<String, JavaParams>>();
    algoParamsList.add(new Tuple2<String, JavaParams>(algo, algoParams));

    Map<String,
      Class<? extends
        LJavaAlgorithm<? extends JavaParams, PreparedData, ?, Query, Prediction>>> algoClassMap =
      new HashMap <> ();
    if (algoName.equals("genericitembased")) {
      algoClassMap.put(algo, GenericItemBased.class);
    } else{
      algoClassMap.put(algo, SVDPlusPlus.class);
    }
*/
/*
    JavaEngine<TrainingData, EmptyParams, TrainingData, Query, Prediction, Actual> engine =
      new JavaEngineBuilder<
        TrainingData, EmptyParams, TrainingData, Query, Prediction, Actual>()
        .dataSourceClass(ItemRecDataSource.class)
        //.preparatorClass(ItemRecPreparator.class)
        //.addAlgorithmClass("genericitembased", GenericItemBased.class)
        //.addAlgorithmClass("svdplusplus", SVDPlusPlus.class)
        //.servingClass(ItemRecServing.class)
        .build();

    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(dsp)
      .build();

    JavaAPIDebugWorkflow.runEngine(
      "Java Itemrec engine",
      3,  // verbose
      engine,
      engineParams,
      null,
      null
      );
*/

    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(dsp)
      .preparatorParams(pp)
      .addAlgorithmParams(algo, algoParams)
      .servingParams(sp)
      .build();

    JavaWorkflow.runEngine(
      (new EngineFactory()).apply(),
      engineParams,
      ItemRecMetrics.class,
      new MetricsParams(k),
      new WorkflowParamsBuilder().batch("Java Itemrec engine").verbose(3).build()
      );
  }

}
