package org.template.similar

import io.prediction.controller.PAlgorithm
import io.prediction.controller.Params
import io.prediction.controller.IPersistentModel
import io.prediction.controller.IPersistentModelLoader
import io.prediction.data.storage.EntityMap

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.SparseVector
import org.apache.spark.mllib.linalg.distributed.MatrixEntry
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.distributed.CoordinateMatrix

import grizzled.slf4j.Logger

import scala.collection.mutable.PriorityQueue

case class DIMSUMAlgorithmParams(val threshold: Double) extends Params

class DIMSUMModel(
    val similarities: RDD[(Int, SparseVector)],
    val items: EntityMap[Item]
  ) extends IPersistentModel[DIMSUMAlgorithmParams] {

  def save(id: String, params: DIMSUMAlgorithmParams,
    sc: SparkContext): Boolean = {

    similarities.saveAsObjectFile(s"/tmp/${id}/similarities")
    sc.parallelize(Seq(items))
      .saveAsObjectFile(s"/tmp/${id}/items")
    true
  }

  override def toString = {
    s"similarities: [${similarities.count()}]" +
    s"(${similarities.take(2).toList}...)" +
    s" items: [${items.size} (${items.take(2).toString}...)]"
  }
}

object DIMSUMModel
  extends IPersistentModelLoader[DIMSUMAlgorithmParams, DIMSUMModel] {
  def apply(id: String, params: DIMSUMAlgorithmParams,
    sc: Option[SparkContext]) = {
    new DIMSUMModel(
      similarities = sc.get.objectFile(s"/tmp/${id}/similarities"),
      items = sc.get
        .objectFile[EntityMap[Item]](s"/tmp/${id}/items").first)
  }
}

class DIMSUMAlgorithm(val ap: DIMSUMAlgorithmParams)
  extends PAlgorithm[PreparedData, DIMSUMModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(data: PreparedData): DIMSUMModel = {
    val itemCount = data.items.size
    // each row is a vector of rated items by this user
    val rows: RDD[Vector] = data.ratings.map ( r =>
      // Convert user and item String IDs to Int index for MLlib
      // (userIndex, (itemIndex, rating))
      (data.users(r.user).toInt, (data.items(r.item).toInt, r.rating))
    ).groupByKey().map { case (u, irIter) =>
      val ir = irIter.toArray.sortBy(_._1)
      // NOTE: index array must be strictly increasing.
      val indexes = ir.map(_._1)
      val values = ir.map(_._2)
      Vectors.sparse(itemCount, indexes, values)
    }

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
      items = data.items
    )
  }

  def predict(model: DIMSUMModel, query: Query): PredictedResult = {
    // convert the white and black list items to Int index
    val whiteList: Option[Set[Int]] = query.whiteList.map( set =>
      set.map(model.items.get(_).map(_.toInt)).flatten
    )
    val blackList: Option[Set[Int]] = query.blackList.map ( set =>
      set.map(model.items.get(_).map(_.toInt)).flatten
    )

    val queryList: Set[Int] = query.items.map(
      model.items.get(_).map(_.toInt)).flatten.toSet

    println(query.categories)

    val indexScores = query.items.flatMap { item =>
      model.items.get(item).map(_.toInt).map { itemInt =>
        val simsSeq = model.similarities.lookup(itemInt)
        if (simsSeq.isEmpty) {
          logger.info(s"No similar items found for ${item}.")
          Array.empty[(Int, Double)]
        } else {
          val sims = simsSeq.head
          sims.indices.zip(sims.values).filter { case (i, v) =>
            whiteList.map(_.contains(i)).getOrElse(true) &&
            blackList.map(!_.contains(i)).getOrElse(true) &&
            // discard items in query as well
            (!queryList.contains(i)) &&
            // filter categories
            query.categories.map { cat =>
              !(model.items.data(i.toLong).categories.toSet
                .intersect(cat).isEmpty)
            }.getOrElse(true)
          }
        }
      }.getOrElse {
        logger.info(s"No similar items for unknown item ${item}.")
        Array.empty[(Int, Double)]
      }
    }

    val aggregatedScores = indexScores.groupBy(_._1)
      .mapValues(_.foldLeft[Double](0)( (b,a) => b + a._2))
      .toList

    val ord = Ordering.by[(Int, Double), Double](_._2).reverse
    val itemScores = getTopN(aggregatedScores, query.num)(ord)
      .map{ case (i, s) =>
        new ItemScore(
          item = model.items(i.toLong),
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
