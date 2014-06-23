package io.prediction.engines.regression

import org.apache.spark.mllib.regression.LinearRegressionWithSGD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.Vector

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD   

import io.prediction.BaseEvaluationDataParams
import io.prediction.core.BaseDataPreparator

// Maybe also remove this subclassing too
class EvalDataParams(val filepath: String, val k: Int)
extends BaseEvaluationDataParams

class DataPrep 
    extends BaseDataPreparator[
        EvalDataParams,
        Null,
        Null,
        RDD[LabeledPoint],
        Vector,
        Double] {
  override
  def prepare(sc: SparkContext, params: EvalDataParams)
  : Map[Int, 
      (Null, Null, RDD[LabeledPoint], RDD[(Vector, Double)])] = {
    val input = sc.textFile(params.filepath)
    val points = input.map { line =>
      val parts = line.split(' ')
      LabeledPoint(
        parts(0).toDouble, 
        Vectors.dense(parts.drop(1).map(_.toDouble)))
    }

    (0 until params.k).map { case ei => {
      (ei, (null, null, points, points.map(p => (p.features, p.label))))
    }}
    .toMap
  }
}


object Runner {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName(s"PredictionIO: Regression")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    val filepath = "data/lr_data.txt"
    val evalDataParams = new EvalDataParams(filepath, 2)

    val dataPrep = new DataPrep
    dataPrep.prepare(sc, evalDataParams).map { case (ei, e) => {
      println(s"$ei $e")
    }}
  }
  
  def good() {
    val conf = new SparkConf().setAppName(s"PredictionIO: Regression")
    conf.set("spark.local.dir", "~/tmp/spark")
    conf.set("spark.executor.memory", "8g")

    val sc = new SparkContext(conf)

    
    // Load and parse the data
    //val data = sc.textFile("mllib/data/ridge-data/lpsa.data")
    val data = sc.textFile("data/lr_data.txt")
    val parsedData = data.map { line =>
      val parts = line.split(' ')
      LabeledPoint(
        parts(0).toDouble, 
        Vectors.dense(parts.drop(1).map(_.toDouble)))
    }

    // Building the model
    val numIterations = 50
    val model = LinearRegressionWithSGD.train(parsedData, numIterations)

    // Evaluate model on training examples and compute training error
    val valuesAndPreds = parsedData.map { point =>
      val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    val MSE = valuesAndPreds.map{case(v, p) => math.pow((v - p), 2)}.mean()
    println("training Mean Squared Error = " + MSE)
  }

}
