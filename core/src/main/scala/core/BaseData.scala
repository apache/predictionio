package io.prediction.core

import io.prediction.{ 
  BaseActual, 
  BaseFeature, 
  BasePrediction,
  BaseValidationUnit,
  BaseTrainingDataParams,
  BaseValidationDataParams,
  BaseValidationResults,
  BaseTrainingData,
  BaseCleansedData
}

import com.twitter.chill.MeatLocker
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

// Base Params
//trait BaseParams extends AnyRef {}
trait BaseParams extends Serializable {}
/*
trait BaseParams extends Serializable {
  private def writeObject(oos: ObjectOutputStream): Unit = {                        
    val boxed = MeatLocker(this)
    oos.writeObject(boxed)                                                                 
  }                                                                                 
                                                                                                
  private def readObject(ois: ObjectInputStream): Unit = {                          
    val params = ois.readObject.asInstanceOf[MeatLocker[BaseParams]]
    initBase(params.get)
  }                                                                                 
}
*/

// Below are internal classes used by PIO workflow
//trait BasePersistentData extends AnyRef {}
trait BasePersistentData extends Serializable {}

trait BaseValidationSeq extends BasePersistentData {}

trait BasePredictionSeq extends BasePersistentData {}

trait BaseValidationUnitSeq extends BasePersistentData {}

trait BaseValidationParamsResults extends BasePersistentData {}

class ValidationSeq[F <: BaseFeature, A <: BaseActual](
  val data: Seq[(F, A)]) extends BaseValidationSeq {}

class PredictionSeq[F <: BaseFeature, P <: BasePrediction, A <: BaseActual](
  val data: Seq[(F, P, A)]) extends BasePredictionSeq {}

class ValidationUnitSeq[VU <: BaseValidationUnit](
  val data: Seq[VU]) extends BaseValidationUnitSeq {}

class ValidationParamsResults[
    TDP <: BaseTrainingDataParams,
    VDP <: BaseValidationDataParams,
    VR <: BaseValidationResults](
    val trainingDataParams: TDP,
    val validationDataParams: VDP,
    val data: VR) extends BaseValidationParamsResults {}

// RDDWrapper
class RDDTD[T <: BaseTrainingData](val v: RDD[T]) 
extends BaseTrainingData

class RDDCD[T <: BaseCleansedData](val v: RDD[T]) 
extends BaseCleansedData


