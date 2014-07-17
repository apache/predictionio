package io.prediction.engines.java.itemrec;

import io.prediction.engines.java.itemrec.algos.GenericItemBased;
import io.prediction.engines.java.itemrec.algos.GenericItemBasedParams;
import io.prediction.engines.java.itemrec.algos.SVDPlusPlus;
import io.prediction.engines.java.itemrec.algos.SVDPlusPlusParams;
import io.prediction.api.Params;
import io.prediction.api.EmptyParams;
import io.prediction.api.java.JavaEngine;
import io.prediction.api.java.JavaEngineParams;
import io.prediction.api.java.JavaEngineParamsBuilder;
import io.prediction.workflow.JavaAPIDebugWorkflow;

import scala.Tuple2;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class Runner {
  public static void main(String[] args) {

    String filePath = "engines/src/main/java/engines/java/itemrec/examples/ratings.csv";
    String algoName = "genericitembased";

    if (args.length == 2) {
      filePath = args[0];
      algoName = args[1];
    }

    System.out.println(Arrays.toString(args));

    DataSourceParams dsp = new DataSourceParams(filePath, 3);
    EmptyParams pp = new EmptyParams();
    GenericItemBasedParams genericItemBasedParams = new GenericItemBasedParams(10);
    SVDPlusPlusParams svdPlusPlusParams = new SVDPlusPlusParams(10);
    ServingParams sp = new ServingParams();

    String algo;
    Params algoParams;
    if (algoName.equals("genericitembased")) {
      algo = "genericitembased";
      algoParams = genericItemBasedParams;
    } else {
      algo = "svdplusplus";
      algoParams = svdPlusPlusParams;
    }

    JavaEngineParams engineParams = new JavaEngineParamsBuilder()
      .dataSourceParams(dsp)
      .preparatorParams(pp)
      .addAlgorithmParams(algo, algoParams)
      .servingParams(sp)
      .build();

    JavaAPIDebugWorkflow.runEngine(
      "Java Itemrec engine",
      3,  // verbose
      (new EngineFactory()).apply(),
      engineParams,
      ItemRecMetrics.class,
      new EmptyParams() // metrics param
      );
  }
}
