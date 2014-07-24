package org.apache.spark.mllib.recommendation.engine

import io.prediction.controller.Engine
import io.prediction.controller.IEngineFactory
import io.prediction.controller.PDataSource
import io.prediction.controller.Params
import io.prediction.controller.PAlgorithm
import io.prediction.controller.IdentityPreparator
import io.prediction.controller.FirstServing
import io.prediction.controller.PersistentParallelModel
import io.prediction.controller.Utils
import io.prediction.workflow.APIDebugWorkflow

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel
import org.json4s._

case class DataSourceParams(val filepath: String) extends Params

case class DataSource(val dsp: DataSourceParams)
  extends PDataSource[DataSourceParams, Null, RDD[Rating], (Int, Int), Double] {

  def read(sc: SparkContext)
  : Seq[(Null, RDD[Rating], RDD[((Int, Int), Double)])] = {
    val data = sc.textFile(dsp.filepath)
    val ratings: RDD[Rating] = data.map(_.split("::") match {
      case Array(user, item, rate) =>
        Rating(user.toInt, item.toInt, rate.toDouble)
    })

    val featureTargets: RDD[((Int, Int), Double)] = ratings.map {
      case Rating(user, product, rate) => ((user, product), rate)
    }

    Seq((null, ratings, featureTargets))
  }
}

case class AlgorithmParams(
  val rank: Int = 10,
  val numIterations: Int = 20,
  val lambda: Double = 0.01) extends Params

class PersistentMatrixFactorizationModel(m: MatrixFactorizationModel)
    extends PersistentParallelModel {

  @transient var model = m
  val rank: Int = m.rank

  def save(id: String): Unit = {
    model.productFeatures.saveAsObjectFile("/tmp/productFeatures")
    model.userFeatures.saveAsObjectFile("/tmp/userFeatures")
  }

  def load(sc: SparkContext, id: String): Unit = {
    model = new MatrixFactorizationModel(
      rank,
      sc.objectFile("/tmp/userFeatures"),
      sc.objectFile("/tmp/productFeatures"))
  }
}

class ALSAlgorithm(val ap: AlgorithmParams)
  extends PAlgorithm[AlgorithmParams, RDD[Rating],
      MatrixFactorizationModel, (Int, Int), Double] {

  def train(data: RDD[Rating]): MatrixFactorizationModel = {
    ALS.train(data, ap.rank, ap.numIterations, ap.lambda)
  }

  def batchPredict(
    model: MatrixFactorizationModel,
    feature: RDD[(Long, (Int, Int))]): RDD[(Long, Double)] = {
    val indexlessFeature = feature.values

    val prediction: RDD[Rating] = model.predict(indexlessFeature)

    val p: RDD[((Int, Int), Double)] = prediction.map {
      r => ((r.user, r.product), r.rating)
    }

    feature.map{ _.swap }
    .join(p)
    .map { case (up, (fi, r)) => (fi,r) }
  }

  def predict(
    model: MatrixFactorizationModel, feature: (Int, Int)): Double = {
    model.predict(feature._1, feature._2)
  }

  @transient override lazy val querySerializer =
    Utils.json4sDefaultFormats + new Tuple2IntSerializer
}

object Run {
  def main(args: Array[String]) {
    val dsp = DataSourceParams("data/movielens.txt")
    val ap = AlgorithmParams()

    APIDebugWorkflow.run(
      dataSourceClassOpt = Some(classOf[DataSource]),
      dataSourceParams = dsp,
      preparatorClassOpt = Some(IdentityPreparator(classOf[DataSource])),
      algorithmClassMapOpt = Some(Map("" -> classOf[ALSAlgorithm])),
      algorithmParamsList = Seq(("", ap)),
      servingClassOpt = Some(FirstServing(classOf[ALSAlgorithm])),
      batch = "Imagine: P Recommendations",
      verbose = 1
    )
  }
}

object RecommendationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      IdentityPreparator(classOf[DataSource]),
      Map("" -> classOf[ALSAlgorithm]),
      FirstServing(classOf[ALSAlgorithm]))
  }
}


class Tuple2IntSerializer extends CustomSerializer[(Int, Int)](format => (
  {
    case JArray(List(JInt(x), JInt(y))) => (x.intValue, y.intValue)
  },
  {
    case x: (Int, Int) => JArray(List(JInt(x._1), JInt(x._2)))
  }
))
