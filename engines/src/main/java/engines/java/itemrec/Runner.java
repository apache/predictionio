package io.prediction.engines.java.itemrec;

import io.prediction.engines.java.itemrec.algos.GenericItemBased;
import io.prediction.engines.java.itemrec.algos.GenericItemBasedParams;
import io.prediction.engines.java.itemrec.algos.SVDPlusPlus;
import io.prediction.engines.java.itemrec.algos.SVDPlusPlusParams;
import io.prediction.engines.java.itemrec.data.TrainingData;
import io.prediction.engines.java.itemrec.data.Feature;
import io.prediction.engines.java.itemrec.data.Prediction;
import io.prediction.BaseParams;
import io.prediction.workflow.JavaDebugWorkflow;
import io.prediction.java.JavaLocalAlgorithm;

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

    EvalParams ep = new EvalParams(filePath, 3, true);
    CleanserParams cp = new CleanserParams();
    GenericItemBasedParams genericItemBasedParams = new GenericItemBasedParams(10);
    SVDPlusPlusParams svdPlusPlusParams = new SVDPlusPlusParams(10);
    ServerParams sp = new ServerParams();

    List<Tuple2<String, BaseParams>> apl = new ArrayList<Tuple2<String, BaseParams>>();
    if (algoName.equals("genericitembased")) {
      apl.add(new Tuple2<String, BaseParams>("genericitembased", genericItemBasedParams));
    } else {
      apl.add(new Tuple2<String, BaseParams>("svdplusplus", svdPlusPlusParams));
    }

    Map<String, Class<? extends JavaLocalAlgorithm<TrainingData, Feature, Prediction,
      ?, ? extends BaseParams>>> algoClassMap = new HashMap<>();

    algoClassMap.put("genericitembased", GenericItemBased.class);
    algoClassMap.put("svdplusplus", SVDPlusPlus.class);

    JavaDebugWorkflow.run(
      "Native Java",
      ItemRecDataPreparator.class,
      ItemRecCleanser.class,
      algoClassMap,
      ItemRecServer.class,
      ItemRecValidator.class,
      ep,
      cp,
      apl,
      sp,
      ep
    );
  }
}
