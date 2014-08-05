package io.prediction.examples.java.itemrec.data;

import java.io.Serializable;
import org.apache.mahout.cf.taste.model.DataModel;

public class PreparedData implements Serializable {
  public DataModel dataModel;

  public PreparedData(DataModel dataModel) {
    this.dataModel = dataModel;
  }
}
