package io.prediction

import io.prediction.core.BasePersistentData
import io.prediction.core.BaseParams
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

// Evaluator Params
trait BaseEvaluationDataParams extends BaseParams {}

trait BaseTrainingDataParams extends BaseParams {}

trait BaseValidationDataParams extends BaseParams {}

trait BaseValidationParams extends BaseParams {}

// Engine Params
trait BaseCleanserParams extends BaseParams {}

trait BaseAlgoParams extends BaseParams {}

trait BaseServerParams extends BaseParams {}

class DefaultCleanserParams() extends BaseCleanserParams{}

//class DefaultServerParams() extends BaseServerParams{}

// Engine Persistent Data
trait BaseCleansedData extends BasePersistentData {}

// TrainingData is a subclass of CleasedData. If an algo can handle (potentially
// unclean training data, it should also handle cleansed data.
trait BaseTrainingData extends BaseCleansedData {}

trait BaseFeature extends BasePersistentData {}

trait BasePrediction extends BasePersistentData {}

trait BaseActual extends BasePersistentData {}

trait BaseModel extends BasePersistentData {}

// Evaluator Persistent Data

trait BaseValidationUnit extends BasePersistentData {}

trait BaseValidationResults extends BasePersistentData {}

trait BaseCrossValidationResults extends BasePersistentData {}

// Concrete helper classes
class EmptyParams() extends BaseParams
with BaseEvaluationDataParams
with BaseTrainingDataParams
with BaseValidationDataParams
with BaseValidationParams
with BaseCleanserParams
with BaseAlgoParams
with BaseServerParams {}

class EmptyData() extends BasePersistentData
with BaseTrainingData
with BaseFeature
with BasePrediction
with BaseActual
with BaseModel
with BaseCleansedData
with BaseValidationUnit
with BaseValidationResults
with BaseCrossValidationResults

object EmptyData {
  def apply() = new EmptyData()
}

