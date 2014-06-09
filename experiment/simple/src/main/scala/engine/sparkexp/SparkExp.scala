package io.prediction.engines.sparkexp

import org.apache.spark.rdd.RDD
import io.prediction.core.

object SparkExpEvaluator extends EvaluatorFactory {

  override def apply(): AbstractEvaluator = {
    new BaseEvaluator(
      classOf[SparkExpDataPreparator],
      classOf[SparkExpValidator])
  }
}

class SparkExpDataPreparator(implicit val sc: SparkContext)
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
    val rand = new Random(0) // random with seed
    val id = Range(0, 10).map(i => s"id${i}").toList
    val d1 = Range(0, 10).map( d => s"${params.s}d1${d}").toList
    val d2 = Range(0, 10).map( d => s"${params.s}d2${d}").toList
    new TD(
      d1 = sc.parallelize(id.zip(d1)),
      d2 = sc.parallelize(id.zip(d2))
    )
  }

  override def prepareValidation(params: VDP): RDD[(F, A)] = {
    val faList: Seq[(F,A)] = List( (new F(f="id1"), new A(a="a1")),
      (new F(f="id2"), new A(a="a2")) )
    sc.parallelize(faList)
  }
}

class SparkExpValidator
  extends SparkValidator[
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
    VU(
      f = feature.f,
      p = predicted.p,
      a = actual.a,
      vu = (feature.f.length + (predicted.p.length - actual.a.length))
    )
  }

}
