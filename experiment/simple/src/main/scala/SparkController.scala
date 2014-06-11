package io.prediction

import org.apache.spark.rdd.RDD

trait SparkCleanser[
    -TD <: BaseTrainingData,
    +CD <: BaseCleansedData,
    CP <: BaseCleanserParams]
    extends Cleanser[TD, CD, CP] {
}

trait SparkAlgorithm[
    -CD <: BaseCleansedData,
    F <: BaseFeature,
    P <: BasePrediction,
    M <: BaseModel,
    AP <: BaseAlgoParams]
    extends Algorithm[CD, F, P, M, AP] {

  def predictBatch(model: M, feature: RDD[(Long, F)]): RDD[(Long, P)]

}


trait SparkServer[-F <: BaseFeature, P <: BasePrediction, SP <: BaseServerParams]
    extends Server[F, P, SP] {

}


trait SparkDataPreparator[
    EDP <: BaseEvaluationDataParams,
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    TD <: BaseTrainingData,
    F <: BaseFeature,
    A <: BaseActual]
    extends DataPreparator[EDP, TDP, VDP, TD, F, A] {

}
