package io.prediction.engines.java.itemrec;

import io.prediction.api.java.LJavaDataSource;
import io.prediction.api.EmptyParams;
import io.prediction.engines.java.itemrec.data.Query;
import io.prediction.engines.java.itemrec.data.Actual;
import io.prediction.engines.java.itemrec.data.TrainingData;

import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.common.TasteException;

import scala.Tuple2;
import scala.Tuple3;
import java.io.File;
import java.lang.Iterable;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// use EmptyParas as DP
public class ItemRecDataSource extends LJavaDataSource<
  DataSourceParams, EmptyParams, TrainingData, Query, Actual> {

  final static Logger logger = LoggerFactory.getLogger(ItemRecDataSource.class);

  DataSourceParams dsp;

  public ItemRecDataSource(DataSourceParams dsp) {
    this.dsp = dsp;
  }

  @Override
  public Iterable<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Actual>>>> read() {
    File ratingFile = new File(dsp.filePath);
    DataModel dataModel = null;
    try {
      DataModel fileDataModel = new FileDataModel(ratingFile);
      // NOTE: convert to GenericDataModel because FileDataModel is not serializable
      // (java.io.NotSerializableException: com.google.common.base.Splitter)
      dataModel = new GenericDataModel(GenericDataModel.toDataMap(fileDataModel));
    } catch (IOException e) {
      logger.error("Caught IOException: " + e.getMessage());
    } catch (TasteException e) {
      logger.error("Caught IOException: " + e.getMessage());
    }
    TrainingData td = new TrainingData(dataModel);

    // TODO generate validation data, hardcode for now...
    List<Tuple2<Query, Actual>> qaList = new ArrayList<Tuple2<Query, Actual>>();
    qaList.add(new Tuple2<Query, Actual>(new Query(1, 10),
      new Actual(new ArrayList<Integer>())));
    qaList.add(new Tuple2<Query, Actual>(new Query(2, 10),
      new Actual(new ArrayList<Integer>())));

    // only one slice
    List<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Actual>>>> data = new
      ArrayList<Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Actual>>>>();

    data.add(new Tuple3<EmptyParams, TrainingData, Iterable<Tuple2<Query, Actual>>>(
      new EmptyParams(), td, qaList));

    return data;
  }

}
