package io.prediction.engines.java.itemrec;

import io.prediction.engines.java.itemrec.data.TrainingData;
import io.prediction.java.JavaLocalCleanser;

// CD same as TD
public class ItemRecCleanser extends JavaLocalCleanser<
  TrainingData, TrainingData, CleanserParams> {

  @Override
  public TrainingData cleanse(TrainingData td) {
    return td;
  }
}
