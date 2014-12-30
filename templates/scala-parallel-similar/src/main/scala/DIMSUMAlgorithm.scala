package org.template.similar

import io.prediction.controller.PAlgorithm
import io.prediction.controller.Params
import io.prediction.controller.IPersistentModel
import io.prediction.controller.IPersistentModelLoader
import io.prediction.data.storage.BiMap

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.SparseVector
import org.apache.spark.mllib.linalg.distributed.MatrixEntry
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.distributed.CoordinateMatrix

import grizzled.slf4j.Logger

import scala.collection.mutable.PriorityQueue

case class DIMSUMAlgorithmParams(val threshold: Double) extends Params

class DIMSUMModel(
    val similarities: RDD[(Int, SparseVector)],
    val userStringIntMap: BiMap[String, Int],
    val itemStringIntMap: BiMap[String, Int]
  ) extends IPersistentModel[DIMSUMAlgorithmParams] {

  def save(id: String, params: DIMSUMAlgorithmParams,
    sc: SparkContext): Boolean = {

    similarities.saveAsObjectFile(s"/tmp/${id}/similarities")
    sc.parallelize(Seq(userStringIntMap))
      .saveAsObjectFile(s"/tmp/${id}/userStringIntMap")
    sc.parallelize(Seq(itemStringIntMap))
      .saveAsObjectFile(s"/tmp/${id}/itemStringIntMap")
    true
  }

  override def toString = {
    s"similarities: [${similarities.count()}]" +
    s"(${similarities.take(2).toList}...)" +
    s" userStringIntMap: [${userStringIntMap.size}]" +
    s"(${userStringIntMap.take(2)}...)" +
    s" itemStringIntMap: [${itemStringIntMap.size}]" +
    s"(${itemStringIntMap.take(2)}...)"
  }
}

object DIMSUMModel
  extends IPersistentModelLoader[DIMSUMAlgorithmParams, DIMSUMModel] {
  def apply(id: String, params: DIMSUMAlgorithmParams,
    sc: Option[SparkContext]) = {
    new DIMSUMModel(
      similarities = sc.get.objectFile(s"/tmp/${id}/similarities"),
      userStringIntMap = sc.get
        .objectFile[BiMap[String, Int]](s"/tmp/${id}/userStringIntMap").first,
      itemStringIntMap = sc.get
        .objectFile[BiMap[String, Int]](s"/tmp/${id}/itemStringIntMap").first)
  }
}

class DIMSUMAlgorithm(val ap: DIMSUMAlgorithmParams)
  extends PAlgorithm[PreparedData, DIMSUMModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(data: PreparedData): DIMSUMModel = {
    // Convert user and item String IDs to Int index for MLlib
    val userStringIntMap = BiMap.stringInt(data.ratings.map(_.user))
    val itemStringIntMap = BiMap.stringInt(data.ratings.map(_.item))
    val itemCount = itemStringIntMap.size
    // each row is a vector of rated items by this user
    val rows = data.ratings.map ( r =>
      //(userIndex, (itemIndex, rating))
      (userStringIntMap(r.user), (itemStringIntMap(r.item), r.rating))
    ).groupByKey().map { case (u, irIter) =>
      val ir = irIter.toArray.sortBy(_._1)
      // NOTE: index array must be strictly increasing.
      val indexes = ir.map(_._1)
      val values = ir.map(_._2)
      Vectors.sparse(itemCount, indexes, values)
    }
    println(userStringIntMap) // debug
    println(itemStringIntMap) // debug
    val mat = new RowMatrix(rows)
    val scores = mat.columnSimilarities(ap.threshold)
    val reversedEntries: RDD[MatrixEntry] = scores.entries
      .map(e => new MatrixEntry(e.j, e.i, e.value))
    val combined = new CoordinateMatrix(scores.entries.union(reversedEntries))
    val similarities = combined.toIndexedRowMatrix.rows
      .map( row => (row.index.toInt, row.vector.asInstanceOf[SparseVector]))

    similarities.foreach(println(_)) // debug
    new DIMSUMModel(
      similarities = similarities,
      userStringIntMap = userStringIntMap,
      itemStringIntMap = itemStringIntMap
    )
  }

  def predict(model: DIMSUMModel, query: Query): PredictedResult = {
    val itemIntStringMap = model.itemStringIntMap.inverse
    // TODO: handle unknown item
    val index = model.itemStringIntMap(query.item)
    val sims = model.similarities.lookup(index).head

    val indexScores = sims.indices.zip(sims.values)

    // largest to smallest order
    val ord = Ordering.by[(Int, Double), Double](_._2).reverse
    val itemScores = getTopN(indexScores, query.num)(ord)
      .map{ case (i, s) =>
        new ItemScore(
          item = itemIntStringMap(i),
          score = s
        )
      }.toArray

    new PredictedResult(itemScores)
  }

  private
  def getTopN[T](s: Seq[T], n: Int)(implicit ord: Ordering[T]): Seq[T] = {

    val q = PriorityQueue()

    for (x <- s) {
      if (q.size < n)
        q.enqueue(x)
      else {
        // q is full
        if (ord.compare(x, q.head) < 0) {
          q.dequeue()
          q.enqueue(x)
        }
      }
    }

    q.dequeueAll.toSeq.reverse
  }
}
