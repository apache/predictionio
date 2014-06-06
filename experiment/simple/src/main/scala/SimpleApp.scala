


/* SimpleApp.scala */
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import scala.util.Random

object SimpleApp {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Simple Application")
    implicit val sc = new SparkContext(conf)

    val tdpVdpList: Seq[(TDP, VDP)] = getParamsSet(EDP())

    val tdpVdpMap: Map[Int, (TDP, VDP)] = (0 to tdpVdpList.size).zip(tdpVdpList).toMap

    val tdpMap: Map[Int, TDP] = tdpVdpMap.mapValues(_._1)
    val vdpMap: Map[Int, VDP] = tdpVdpMap.mapValues(_._2)

    // trainig data
    val tdMap: Map[Int, TD] = tdpMap.map { case (idx, tdp) =>
      (idx, prepareTraining(tdp))
    }

    // model
    val mMap: Map[Int, M] = tdMap.map { case (idx, td) =>
      (idx, train(td))
    }

    // feature and actual
    val faMap: Map[Int, RDD[(Long, (F,A))]] = vdpMap.map{ case (idx, vdp) =>
      (idx, prepareValidation(vdp).zipWithIndex().map(_.swap))
    }

    // batch prediction
    val pMap: Map[Int, RDD[(Long, P)]] = faMap.map { case (idx, fa) =>
      val feature: RDD[(Long, F)] = fa.map{ case (k, (f,a)) => (k, f) }
      val predicted: RDD[(Long, P)] = predictBatch(mMap(idx), feature)
      (idx, predicted)
    }

    // validation result
    val vrList: Seq[(TDP, VDP, VR)] = faMap.map{ case (idx, fa) =>
      val predicted: RDD[(Long, P)] = pMap(idx)
      val vu: RDD[VU] = fa.join(predicted) // (k, ((f,a), p))
        .map{ case (k, ((f, a), p)) =>
          validate(f, p, a)
        }
      val tdp = tdpMap(idx)
      val vdp = vdpMap(idx)
      val vr = validateSet(tdp, vdp, vu)
      (tdp, vdp, vr)
    }.toList

    val cvr = crossValidate(vrList)

    println(cvr.cvr)

  }

  case class EDP(

  )
  case class TDP(
    s: String
  )
  case class VDP(
    s: String
  )

  case class TD(
    d1: RDD[(String, String)],
    d2: RDD[(String, String)]
  )

  case class M(
    m1: RDD[(String, String)],
    m2: RDD[(String, String)]
  )

  case class F(
    f: String
  )

  case class P(
    p: String
  )

  case class A(
    a: String
  )

  case class VU(
    f: String,
    p: String,
    a: String,
    vu: Int
  )

  case class VR(
    vr: Int
  )

  case class CVR(
    cvr: Double
  )

  def getParamsSet(params: EDP): Seq[(TDP, VDP)] = {
    List((TDP(s="s1"), VDP(s="v1")), (TDP(s="s2"), VDP(s="v1")))
  }

  def prepareTraining(params: TDP)(implicit sc: SparkContext): TD = {
    val rand = new Random(0) // random with seed
    val id = Range(0, 10).map(i => s"id${i}").toList
    val d1 = Range(0, 10).map( d => s"${params.s}d1${d}").toList
    val d2 = Range(0, 10).map( d => s"${params.s}d2${d}").toList
    TD(
      d1 = sc.parallelize(id.zip(d1)),
      d2 = sc.parallelize(id.zip(d2))
    )
  }

  def prepareValidation(params: VDP)(implicit sc: SparkContext): RDD[(F, A)] = {
    val faList: Seq[(F,A)] = List( (F(f="id1"),A(a="a1")), (F(f="id2"), A(a="a2")) )
    sc.parallelize(faList)
  }

  def train(td: TD): M = {
    // dummy RDD processing
    val mod1: RDD[(String, String)] = td.d1.map{ case (id, x) => (id, s"${x}m1") }
    val mod2: RDD[(String, String)] = td.d2.map{ case (id, x) => (id, s"${x}m2") }
    M(
     m1 = mod1,
     m2 = mod2
    )
  }

  def predict(model: M, feature: F): P = {
    val m1 = model.m1.lookup(feature.f)
    val m2 = model.m2.lookup(feature.f)
    val res = (m1.mkString(" ") + m2.mkString(" ") + feature.f)
    P(
     p = res
   )
  }

  def predictBatch(model: M, feature: RDD[(Long, F)]): RDD[(Long, P)] = {
    feature.map{case(idx, f) => (f.f, idx)}
      .join(model.m1) // (f.f, (idx, m1.d))
      .join(model.m2) // (f.f, ((idx, m1.d), m2.d))
      .map { case (f, ((idx, d1), d2)) =>
        val res = s"${d1}+${d2}+${f}"
        (idx, P(p=res))
      }
  }

  def validate(feature: F, predicted: P, actual: A): VU = {
    VU(
      f = feature.f,
      p = predicted.p,
      a = actual.a,
      vu = (feature.f.length + (predicted.p.length - actual.a.length))
    )
  }

  def validateSet(
    trainingDataParams: TDP,
    validationDataParams: VDP,
    validationUnits: RDD[VU]): VR = {
      val res = validationUnits.map{ vu => vu.vu }.reduce(_ + _)
      validationUnits.collect()
        .foreach{ case vu =>
          println(s"${vu.f} ${vu.p} ${vu.a}")
      }
      VR(vr = res)
    }

  def crossValidate(validationResultsSeq: Seq[(TDP, VDP, VR)]): CVR = {
    CVR(
      cvr = validationResultsSeq.map(_._3.vr).sum.toDouble/(validationResultsSeq.size)
    )
  }

}
