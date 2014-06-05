package io.prediction.core

// FIXME(yipjustin). I am lazy...
import io.prediction._
import scala.reflect.Manifest

import com.twitter.chill.MeatLocker

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream

trait AbstractParameterizedDoer extends Serializable {
  private var baseParams: BaseParams = null
  //def paramClass(): Manifest[_ <: BaseParams]

  def initBase(baseParams: BaseParams): Unit = {
    this.baseParams = baseParams
  }

  private def writeObject(oos: ObjectOutputStream): Unit = {                        
    val boxed = MeatLocker(baseParams)
    oos.writeObject(boxed)                                                                 
  }                                                                                 
                                                                                                
  private def readObject(ois: ObjectInputStream): Unit = {                          
    val params = ois.readObject.asInstanceOf[MeatLocker[BaseParams]]
    initBase(params.get)
  }                                                                                 
}



//

trait AbstractCleanser {

  def initBase(baseCleanserParams: BaseCleanserParams): Unit

  def paramsClass(): Manifest[_ <: BaseCleanserParams]

  def cleanseBase(trainingData: BaseTrainingData): BaseCleansedData

}

trait AbstractAlgorithm extends AbstractParameterizedDoer {

  //abstract override def initBase(baseAlgoParams: BaseAlgoParams): Unit
  //def initBase(baseParams: BaseParams): Unit

  def paramsClass(): Manifest[_ <: BaseAlgoParams]

  def trainBase(cleansedData: BaseCleansedData): BaseModel

  def predictSeqBase(baseModel: BaseModel, validationSeq: BaseValidationSeq)
    : BasePredictionSeq

}

trait AbstractServer {

  def initBase(baseServerParams: BaseServerParams): Unit

  def paramsClass(): Manifest[_ <: BaseServerParams]

  // The server takes a seq of Prediction and combine it into one.
  // In the batch model, things are run in batch therefore we have seq of seq.
  def combineSeqBase(basePredictionSeqSeq: Seq[BasePredictionSeq])
    : BasePredictionSeq
}

class AbstractEngine(

  val cleanserClass: Class[_ <: AbstractCleanser],

  val algorithmClassMap: Map[String, Class[_ <: AbstractAlgorithm]],

  val serverClass: Class[_ <: AbstractServer]) {

}
