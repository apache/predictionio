package io.prediction.engines.java.itemrec;

import io.prediction.api.EmptyParams;
import io.prediction.api.java.JavaMetrics;
import io.prediction.engines.java.itemrec.data.Query;
import io.prediction.engines.java.itemrec.data.Prediction;
import io.prediction.engines.java.itemrec.data.Actual;

import scala.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemRecMetrics extends JavaMetrics<
  EmptyParams, EmptyParams, Query, Prediction, Actual, Float, Float, Float> {

  final static Logger logger = LoggerFactory.getLogger(ItemRecMetrics.class);

  @Override
  public Float computeUnit(Query query, Prediction predicted, Actual actual) {
    logger.info(query.toString());
    logger.info(predicted.toString());
    return 0.0f; // TODO
  }

  @Override
  public Float computeSet(EmptyParams dataParams, Iterable<Float> metricUnits) {
    return 0.0f; //TODO
  }

  @Override
  public Float computeMultipleSets(Iterable<Tuple2<EmptyParams, Float>> input) {
    return 0.0f; // TODO
  }

}
