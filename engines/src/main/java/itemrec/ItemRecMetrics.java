package io.prediction.engines.java.itemrec;

import io.prediction.controller.EmptyParams;
import io.prediction.controller.java.JavaMetrics;
import io.prediction.engines.java.itemrec.data.Query;
import io.prediction.engines.java.itemrec.data.Prediction;
import io.prediction.engines.java.itemrec.data.Actual;
import io.prediction.engines.util.MathUtil;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import scala.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemRecMetrics extends JavaMetrics<
  EmptyParams, EmptyParams, Query, Prediction, Actual, Double, Double, String> {

  final static Logger logger = LoggerFactory.getLogger(ItemRecMetrics.class);

  @Override
  public Double computeUnit(Query query, Prediction predicted, Actual actual) {
    logger.info("computeUnit");
    logger.info(query.toString());
    logger.info(predicted.toString());
    Double ap = MathUtil.jAveragePrecisionAtK(20, predicted.iids, actual.iids);
    logger.info(ap.toString());
    return ap;
  }

  @Override
  public Double computeSet(EmptyParams dataParams, Iterable<Double> metricUnits) {
    Double sum = 0.0;
    int size = 0;
    logger.info("computeSet");
    Iterator<Double> it = metricUnits.iterator();
    while (it.hasNext()) {
      sum += it.next();
      size += 1;
    }

    Double mean = (sum / size);
    logger.info(mean.toString());
    return mean;
  }

  @Override
  public String computeMultipleSets(Iterable<Tuple2<EmptyParams, Double>> input) {
    logger.info("computeMultipleSets");
    Iterator<Tuple2<EmptyParams, Double>> it = input.iterator();
    List<String> output = new ArrayList<String>();
    while (it.hasNext()) {
      output.add(it.next()._2().toString());
    }
    logger.info(output.toString());
    return output.toString();
  }

}
