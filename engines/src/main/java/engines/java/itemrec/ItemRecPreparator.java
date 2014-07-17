package io.prediction.engines.java.itemrec;

import io.prediction.engines.java.itemrec.data.TrainingData;
import io.prediction.api.java.LJavaPreparator;
import io.prediction.api.EmptyParams;

// PD same as TD
public class ItemRecPreparator extends LJavaPreparator<
  EmptyParams, TrainingData, TrainingData> {

  @Override
  public TrainingData prepare(TrainingData td) {
    return td;
  }
}
