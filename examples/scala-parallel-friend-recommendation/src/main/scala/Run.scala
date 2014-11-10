package io.prediction.examples.pfriendrecommendation

import io.prediction.controller.Engine
import io.prediction.controller.IEngineFactory
import io.prediction.controller.IPersistentModel
import io.prediction.controller.IPersistentModelLoader
import io.prediction.controller.PDataSource
import io.prediction.controller.Params
import io.prediction.controller.P2LAlgorithm
import io.prediction.controller.PAlgorithm
import io.prediction.controller.IdentityPreparator
import io.prediction.controller.FirstServing
import io.prediction.controller.Utils
import io.prediction.controller.Workflow
import io.prediction.controller.WorkflowParams

import org.apache.commons.io.FileUtils
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
// For stubbing batchpredict
import org.apache.spark.rdd.EmptyRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._
import org.json4s._

import scala.io.Source

import java.io.File

/*
  Data Source Params : path to data file
 */
case class DataSourceParams(
  val filepath: String
) extends Params

/*
  Data Source produces loads graph from file specified in dsp and generates
  GraphX object

  About the tuple query class: this is how you format the query : 
  `curl -H "Content-Type: application/json" -d '[1,4]'
  http://localhost:8000/queries.json`

  About the 'actual value' class: this is how you read test data. Basically the
  format is query/input -> actual value. 
  */
case class DataSource(
  val dsp: DataSourceParams
) extends PDataSource[DataSourceParams, Null, Graph[Int,Int], (Int, Int), Double] {
  def read(sc: SparkContext) : Seq[(Null, Graph[Int,Int], RDD[((Int,Int),Double)])] = {
    val data = sc.textFile(dsp.filepath)
    // Vertex maintain original ID
    val g = GraphLoader.edgeListFile(sc, dsp.filepath)
    Seq((null, g, sc.parallelize(List())))
  }
}

case class AlgorithmParams(
  // Suggested by a couple papers to used number iterations = 6
  val numIterations: Int = 6,
  val scalar: Double = 0.7
) extends Params

/*
  Model for SimRank
  */
case class SimRankModel (
  val g: Graph[Int,Int],
  val memo: Array[Array[Double]]
) extends Serializable

class SimRankAlgorithm(val ap: AlgorithmParams)
  // P2L is tmp. Want stub out the batch predict for now
  extends PAlgorithm[AlgorithmParams, Graph[Int,Int], SimRankModel, (Int, Int), Double] {

  def getAdjacentVertexId(v :VertexId, g: Graph[Int,Int]): RDD[VertexId] = {
    g.edges.filter( (e:Edge[Int]) => e.srcId == v)
           .map( (e:Edge[Int]) => e.dstId )
  }

  def train(gd: Graph[Int,Int]): SimRankModel = {
    // base case
    var prevIter = Array.ofDim[Double](gd.vertices.count().toInt, gd.vertices.count().toInt)
    // prevIter.cache
    // sc.parallelize(prevIter)

    for(iter <- 0 until ap.numIterations) {
      // Sim(a,a) = 1
      for(index <- 0 until gd.vertices.count().toInt)
        prevIter(index)(index) = 1

      var currIter = Array.ofDim[Double](gd.vertices.count().toInt, gd.vertices.count().toInt)
      for(x <- gd.vertices.toArray; y <- gd.vertices.toArray) {
        // Using filtering instead of mapReduceTriplets. MRT is not suitable for
        // pairwise neighborhood comparison.
        // ._1 index is the vertexId
        val xNeighbors = getAdjacentVertexId(x._1, gd)
        val yNeighbors = getAdjacentVertexId(y._1, gd)
        // Normalization factor
        var scalar = ap.scalar / (xNeighbors.count() * yNeighbors.count())
        for(srcId <- xNeighbors.toArray; dstId <- yNeighbors.toArray) {
          currIter(x._1.toInt)(y._1.toInt) += scalar * prevIter(srcId.toInt)(dstId.toInt)
        }
      }

      prevIter = currIter
      
    }

    new SimRankModel(gd, prevIter)
  }


  def batchPredict(
    model: SimRankModel,
    feature: RDD[(Long, (Int, Int))]): RDD[(Long, Double)] = {
    feature.map(x => (x._1, predict(model, (x._2._1, x._2._1))))
  }

  def predict(
    model: SimRankModel, feature: (Int, Int)): Double = {
    var score = -1.0
    // Quick case : sim(a,a) = 1.0
    if (feature._1 == feature._2) {
      score = 1.0
    } else {

      // Translate to vertexID
      val v = model.g.vertices.filter((vertex: (VertexId, Int)) => {
          vertex._1 == feature._1 || vertex._1 == feature._2 
       }).toArray

      if (v.size == 2) {
        // Look up simrank score in model using vertexID
        // Leveraged reflexivity of simrank to skip the check for src/dst vertex
        // mapping to v's vertices
        score = model.memo(v(0)._1.toInt)(v(1)._1.toInt)
      } else {
        // This is the case when the query does not match any nodes from our
        // data
        score = v.size * -1
      }
   }
   score
 }

  @transient override lazy val querySerializer =
    Utils.json4sDefaultFormats + new Tuple2IntSerializer
}

object Run {
  def main(args: Array[String]) {
    val dsp = DataSourceParams("data/generic_graph.txt")
    val ap = AlgorithmParams()

    Workflow.run(
      dataSourceClassOpt = Some(classOf[DataSource]),
      dataSourceParams = dsp,
      preparatorClassOpt = Some(IdentityPreparator(classOf[DataSource])),
      algorithmClassMapOpt = Some(Map("" -> classOf[SimRankAlgorithm])),
      algorithmParamsList = Seq(("", ap)),
      servingClassOpt = Some(FirstServing(classOf[SimRankAlgorithm])),
      params = WorkflowParams(
	      batch = "Imagine: P Recommendations",
        verbose = 1
      )
    )
  }
}

object PSimRankEngineFactory extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      IdentityPreparator(classOf[DataSource]),
      Map("" -> classOf[SimRankAlgorithm]),
      FirstServing(classOf[SimRankAlgorithm]))
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
