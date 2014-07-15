package io.prediction.engines.java.itemrec;

import io.prediction.java.JavaLocalDataPreparator;
import io.prediction.engines.java.itemrec.data.Feature;
import io.prediction.engines.java.itemrec.data.Actual;
import io.prediction.engines.java.itemrec.data.TrainingData;

import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;

import scala.Tuple2;
import java.io.File;
import java.lang.Iterable;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// temporarily use EvalParams for TDP and VDP
public class ItemRecDataPreparator extends JavaLocalDataPreparator<
  EvalParams, EvalParams, EvalParams, TrainingData, Feature, Actual> {

  final static Logger logger = LoggerFactory.getLogger(ItemRecDataPreparator.class);

  @Override
  public Iterable<Tuple2<EvalParams, EvalParams>> getParamsSet(EvalParams edp) {
    Tuple2<EvalParams, EvalParams> t = new Tuple2<EvalParams, EvalParams>(edp, edp);
    List<Tuple2<EvalParams, EvalParams>> p = new ArrayList<Tuple2<EvalParams, EvalParams>>();
    p.add(t);
    return p;
  }

  @Override
  public TrainingData prepareTraining(EvalParams tdp) {
    File ratingFile = new File(tdp.filePath);
    DataModel dataModel = null;
    try {
      dataModel = new FileDataModel(ratingFile);
    } catch (IOException e) {
      logger.error("Caught IOException: " + e.getMessage());

    }
    return new TrainingData(dataModel);
  }

  @Override
  public Iterable<Tuple2<Feature, Actual>> prepareValidation(EvalParams vdp) {
    // TODO generate validation data
    return new ArrayList<Tuple2<Feature, Actual>>();
  }
}
