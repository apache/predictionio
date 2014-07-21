package io.prediction.engines.java.itemrec;

import io.prediction.engines.java.itemrec.data.PreparedData;
import io.prediction.engines.java.itemrec.data.TrainingData;
import io.prediction.controller.java.LJavaPreparator;
import io.prediction.controller.EmptyParams;
import io.prediction.engines.util.MahoutUtil;
import io.prediction.engines.util.MahoutUtil.Rating;

import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;


// PD same as TD
public class ItemRecPreparator extends LJavaPreparator<
  EmptyParams, TrainingData, PreparedData> {

  @Override
  public PreparedData prepare(TrainingData td) {
    DataModel dataModel = MahoutUtil.buildDataModel(td.ratings);
    PreparedData pd = new PreparedData(dataModel);
    return pd;
  }
}
