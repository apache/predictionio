package io.prediction.engines.java.regression;

import io.prediction.java.JavaLocalDataPreparator;
import io.prediction.java.JavaSimpleLocalDataPreparator;

import scala.Tuple2;

import java.util.List;
import java.util.ArrayList;
import java.lang.Iterable;

public class SimpleDataPreparator
  extends JavaSimpleLocalDataPreparator<
    EvaluationDataParams,
    Integer,
    Integer,
    Integer> {

  public Tuple2<Integer, Iterable<Tuple2<Integer, Integer>>> 
    prepare(EvaluationDataParams edp) {
    Integer td = new Integer(100);

    List<Tuple2<Integer, Integer>> faList = 
      new ArrayList<Tuple2<Integer, Integer>>();

    faList.add(new Tuple2(new Integer(1), new Integer(20)));
    faList.add(new Tuple2(new Integer(4), new Integer(14)));

    return new Tuple2(td, faList);
  }
}

