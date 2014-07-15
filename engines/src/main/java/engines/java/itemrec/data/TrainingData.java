package io.prediction.engines.java.itemrec.data;

import java.io.Serializable;
import org.apache.mahout.cf.taste.model.DataModel;

public class TrainingData implements Serializable {
  public DataModel dataModel;

  public TrainingData(DataModel dataModel) {
    this.dataModel = dataModel;
  }
}
