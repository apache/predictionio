package io.prediction.engines.sparkexp

import io.prediction.core.AbstractEvaluator
import io.prediction.core.BaseEvaluator
import io.prediction.SparkDataPreparator
import io.prediction.EvaluatorFactory
import io.prediction.Validator

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

object SparkExpEvaluator extends EvaluatorFactory {

  override def apply(): AbstractEvaluator = {
    new BaseEvaluator(
      classOf[SparkExpDataPreparator],
      classOf[SparkExpValidator])
  }
}

class SparkExpDataPreparator(val sc: SparkContext)
  extends SparkDataPreparator[
    EDP,
    TDP,
    VDP,
    TD,
    F,
    A
  ] {

  override def getParamsSet(params: EDP): Seq[(TDP, VDP)] = {
    List(( new TDP(s="s1"), new VDP(s="v1")),
      (new TDP(s="s2"), new VDP(s="v1")))
  }

  override def prepareTraining(params: TDP): TD = {
    val id = Range(0, 10).map(i => s"id${i}").toList
    val d1 = Range(0, 10).map( d => s"${params.s}d1${d}").toList
    val d2 = Range(0, 10).map( d => s"${params.s}d2${d}").toList
    new TD(
      d1 = sc.parallelize(id.zip(d1)),
      d2 = sc.parallelize(id.zip(d2))
    )
  }

  override def prepareValidation(params: VDP): Seq[(F, A)] = {
    val faList: Seq[(F,A)] = List( (new F(f="id1"), new A(a="a1")),
      (new F(f="id2"), new A(a="a2")) )
    faList
  }
}

class SparkExpValidator
  extends Validator[
    EDP,
    TDP,
    VDP,
    F,
    P,
    A,
    VU,
    VR,
    CVR
  ] {

  override def validate(feature: F, predicted: P, actual: A): VU = {
    new VU(
      f = feature.f,
      p = predicted.p,
      a = actual.a,
      vu = (feature.f.length + (predicted.p.length - actual.a.length))
    )
  }

  override def validateSet(
    trainingDataParams: TDP,
    validationDataParams: VDP,
    validationUnits: Seq[VU]): VR = {
      val res = validationUnits.map{ vu => vu.vu }.reduce(_ + _)
      new VR(vr = res)
    }

  override def crossValidate(
    validationResultsSeq: Seq[(TDP, VDP, VR)]): CVR = {
      val total = validationResultsSeq.map(_._3.vr).sum.toDouble
      val size = validationResultsSeq.size
      new CVR(
        cvr = total/size
      )
  }
}
