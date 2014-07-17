package io.prediction.workflow

import scala.language.existentials

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEngine

//import io.prediction.BaseEvaluationDataParams

import com.github.nscala_time.time.Imports.DateTime

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream

import io.prediction.core._
import io.prediction._

import org.apache.spark.rdd.RDD

import scala.reflect.Manifest

import com.twitter.chill.Externalizer

object SparkWorkflow {
  @deprecated("Please use EvaluationWorkflow", "20140624")
  def run[
      EDP <: BaseParams : Manifest,
      VP <: BaseParams : Manifest,
      TDP <: BaseParams : Manifest,
      VDP <: BaseParams : Manifest,
      TD: Manifest,
      NTD : Manifest,
      NCD : Manifest,
      F : Manifest,
      NF : Manifest,
      P : Manifest,
      NP : Manifest,
      A : Manifest,
      VU : Manifest,
      VR : Manifest,
      CVR <: AnyRef : Manifest](
    batch: String,
    evalDataParams: BaseParams,
    validationParams: BaseParams,
    cleanserParams: BaseParams,
    algoParamsList: Seq[(String, BaseParams)],
    serverParams: BaseParams,
    baseEngine: BaseEngine[NTD,NCD,NF,NP],
    baseEvaluator: BaseEvaluator[EDP,VP,TDP,VDP,TD,F,P,A,VU,VR,CVR]
    ): (Array[Array[Any]], Seq[(BaseParams, BaseParams, VR)], CVR) = {
    EvaluationWorkflow.run(
      batch,
      Map[String, String](),
      evalDataParams,
      validationParams,
      cleanserParams,
      algoParamsList,
      serverParams,
      baseEngine,
      baseEvaluator)
  }
}
