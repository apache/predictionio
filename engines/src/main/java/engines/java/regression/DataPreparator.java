package io.prediction.engines.java.regression;

import io.prediction.java.JavaLocalDataPreparator;

import scala.Tuple2;

import java.util.List;
import java.util.ArrayList;
import java.lang.Iterable;

public class DataPreparator
  extends JavaLocalDataPreparator<
    EvaluationDataParams,
    EvaluationDataParams,
    EvaluationDataParams,
    Integer,
    Integer,
    Integer> {

  public List<Tuple2<EvaluationDataParams, EvaluationDataParams>> 
    getParamsSet(EvaluationDataParams params) {
    List<Tuple2<EvaluationDataParams, EvaluationDataParams>> l = 
      new ArrayList<Tuple2<EvaluationDataParams, EvaluationDataParams>>();
    l.add(new Tuple2<EvaluationDataParams, EvaluationDataParams>(
          new EvaluationDataParams(10, 4),
          new EvaluationDataParams(10, 4)));
    return l;
  }

  public Integer prepareTraining(EvaluationDataParams params) {
    return new Integer(params.n);
  }

  public List<Tuple2<Integer, Integer>> 
    prepareValidation(EvaluationDataParams params) {
    List<Tuple2<Integer, Integer>> l = 
      new ArrayList<Tuple2<Integer, Integer>>();

    l.add(new Tuple2<Integer, Integer>(params.e, params.n));
    l.add(new Tuple2<Integer, Integer>(params.n, params.e));
    return l;
  }
}

