package io.prediction.core

import io.prediction.controller.Utils
import io.prediction.controller.EngineParams
import io.prediction.controller.WorkflowParams

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import scala.reflect._
import org.json4s.JValue

abstract class BaseEngine[EI, Q, P, A] extends Serializable {
  // Return Persist-able models
  def train(
    sc: SparkContext, 
    engineParams: EngineParams,
    engineInstanceId: String = "",
    params: WorkflowParams = WorkflowParams()): Seq[Any]

  def eval(sc: SparkContext, engineParams: EngineParams)
  : Seq[(EI, RDD[(Q, P, A)])]

  def jValueToEngineParams(variantJson: JValue): EngineParams = {
    throw new NotImplementedError("json to EngineParams is not implemented.")
    EngineParams()  
  }
}
