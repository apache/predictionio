package io.prediction.java

import io.prediction.core.LocalDataPreparator
import io.prediction.BaseParams
import java.util.{ List => JList }
import java.lang.{ Iterable => JIterable }
import scala.collection.JavaConversions._

// Injecting fake manifest to superclass
abstract class JavaLocalDataPreparator[
    EDP <: BaseParams,
    TDP <: BaseParams,
    VDP <: BaseParams,
    TD, F, A]
    extends LocalDataPreparator[EDP, TDP, VDP, TD, F, A]()(
        manifest[AnyRef].asInstanceOf[Manifest[EDP]], 
        manifest[AnyRef].asInstanceOf[Manifest[TDP]], 
        manifest[AnyRef].asInstanceOf[Manifest[TD]]
    ){

  def getParamsSet(params: EDP): Seq[(TDP, VDP)] = {
    jGetParamsSet(params).toSeq
  }

  // Use Tuple2 to make it explicitly clear for java builders.
  def jGetParamsSet(params: EDP): JIterable[Tuple2[TDP, VDP]]
  
  def prepareTraining(params: TDP): TD = jPrepareTraining(params)
  
  def jPrepareTraining(params: TDP): TD

  def prepareValidation(params: VDP): Seq[Tuple2[F, A]] = {
    jPrepareValidation(params).toSeq
  }

  def jPrepareValidation(params: VDP): JIterable[Tuple2[F, A]]

}




