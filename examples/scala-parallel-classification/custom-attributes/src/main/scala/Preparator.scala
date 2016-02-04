package com.test1

import io.prediction.controller.PPreparator

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.regression.LabeledPoint

case class PreparedData(
  labeledPoints: RDD[LabeledPoint],
  gendersMap: Map[String,Double],
  educationMap: Map[String,Double]
)

class Preparator extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(
      trainingData.labeledPoints,
      trainingData.gendersMap,
      trainingData.educationMap)
  }
}
