package io.prediction.engines.java.itemrec;

import scala.Tuple3;
import io.prediction.java.JavaValidator;

import io.prediction.engines.java.itemrec.data.Feature;
import io.prediction.engines.java.itemrec.data.Prediction;
import io.prediction.engines.java.itemrec.data.Actual;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ItemRecValidator extends JavaValidator<
  EvalParams,
  EvalParams,
  EvalParams,
  Feature, Prediction, Actual, Float, Float, Float> {

  final static Logger logger = LoggerFactory.getLogger(ItemRecValidator.class);

  @Override
  public Float validate(Feature feature, Prediction predicted, Actual actual) {
    logger.info(feature.toString());
    logger.info(predicted.toString());
    return 0.0f; // TODO
  }

  @Override
  public Float validateSet(
  EvalParams trainingDataParams,
  EvalParams validationDataParams,
  Iterable<Float> validationUnits) {
    return 0.0f; //TODO
  }

  @Override
  public Float crossValidate(
    Iterable<Tuple3<EvalParams, EvalParams, Float>> validateResultsSeq) {

    return 0.0f; // TODO
  }

}
