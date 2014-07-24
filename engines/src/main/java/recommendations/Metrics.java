package  io.prediction.engines.java.recommendations;

import io.prediction.controller.java.JavaMetrics;
import io.prediction.controller.EmptyParams;

import scala.Tuple2;
import java.util.Arrays;
import org.apache.commons.collections.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Root mean square error */
public class Metrics
  extends JavaMetrics<EmptyParams, EmptyParams, Query, Float, Float,
  Double, Double, String> {

  final static Logger logger = LoggerFactory.getLogger(Metrics.class);

  @Override
  public Double computeUnit(Query query, Float predicted, Float actual) {
    logger.info("Q: " + query.toString() + " P: " + predicted + " A: " + actual);
    // return squared error
    double error = predicted - actual;
    return (error * error);
  }

  @Override
  public Double computeSet(EmptyParams dataParams, Iterable<Double> metricUnits) {
    double sum = 0.0;
    int count = 0;
    for (double squareError : metricUnits) {
      sum += squareError;
      count += 1;
    }
    return Math.sqrt(sum / count);
  }

  @Override
  public String computeMultipleSets(
    Iterable<Tuple2<EmptyParams, Double>> input) {
    return Arrays.toString(IteratorUtils.toArray(input.iterator()));
  }
}
