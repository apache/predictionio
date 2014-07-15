package io.prediction.engines.java.itemrec;

import io.prediction.engines.java.itemrec.algos.GenericItemBased;
import io.prediction.engines.java.itemrec.algos.GenericItemBasedParams;
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

public class Runner {
  public static void main(String[] args) {
    EvalParams ep = new EvalParams("/tmp/pio/ratings.csv", 3, true);
    CleanserParams cp = new CleanserParams();
    GenericItemBasedParams ap = new GenericItemBasedParams(10);
    ServerParams sp = new ServerParams();

    List<Tuple2<String, BaseParams>> apl = new ArrayList<Tuple2<String, BaseParams>>();
    apl.add(new Tuple2<String, BaseParams>("genericitembased", ap));

    Map<String, Class<? extends JavaLocalAlgorithm<TrainingData, Feature, Prediction,
      ?, ? extends BaseParams>>> algoClassMap = new HashMap<>();

    algoClassMap.put("genericitembased", GenericItemBased.class);

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
