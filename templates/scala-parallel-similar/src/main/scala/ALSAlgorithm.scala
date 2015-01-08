package org.template.similar

import io.prediction.controller.PAlgorithm
import io.prediction.controller.Params
import io.prediction.controller.IPersistentModel
import io.prediction.controller.IPersistentModelLoader
import io.prediction.data.storage.BiMap

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.{Rating => MLlibRating}
import org.apache.spark.mllib.linalg.distributed.MatrixEntry
import org.apache.spark.mllib.linalg.distributed.CoordinateMatrix

import grizzled.slf4j.Logger

import scala.collection.mutable.PriorityQueue

case class ALSAlgorithmParams(
  val rank: Int,
  val numIterations: Int,
  //val lambda: Double,
  val threshold: Double // for DIMSUM
) extends Params

class ALSModel(
  val similarities: RDD[(Int, Vector[(Int, Double)])],
  val itemStringIntMap: BiMap[String, Int],
  val items: Map[Int, Item]
) extends IPersistentModel[ALSAlgorithmParams] {

  @transient lazy val itemIntStringMap = itemStringIntMap.inverse

  def save(id: String, params: ALSAlgorithmParams,
    sc: SparkContext): Boolean = {

    similarities.saveAsObjectFile(s"/tmp/${id}/similarities")
    sc.parallelize(Seq(itemStringIntMap))
      .saveAsObjectFile(s"/tmp/${id}/itemStringIntMap")
    sc.parallelize(Seq(items))
      .saveAsObjectFile(s"/tmp/${id}/items")
    true
  }

  override def toString = {
    s"similarities: [${similarities.count()}]" +
    s"(${similarities.take(2).toList}...)" +
    s" itemStringIntMap: [${itemStringIntMap.size}]" +
    s"(${itemStringIntMap.take(2).toString}...)]" +
    s" items: [${items.size}]" +
    s"(${items.take(2).toString}...)]"
  }
}

object ALSModel
  extends IPersistentModelLoader[ALSAlgorithmParams, ALSModel] {
  def apply(id: String, params: ALSAlgorithmParams,
    sc: Option[SparkContext]) = {
    new ALSModel(
      similarities = sc.get.objectFile(s"/tmp/${id}/similarities"),
      itemStringIntMap = sc.get
        .objectFile[BiMap[String, Int]](s"/tmp/${id}/itemStringIntMap").first,
      items = sc.get
        .objectFile[Map[Int, Item]](s"/tmp/${id}/items").first)
  }
}


/**
  * Use ALS to build item x feature matrix and run DIMSUM column similarity
  */
class ALSAlgorithm(val ap: ALSAlgorithmParams)
  extends PAlgorithm[PreparedData, ALSModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(data: PreparedData): ALSModel = {
    // create User and item's String ID to integer index BiMap
    val userStringIntMap = BiMap.stringInt(data.users.keys)
    val itemStringIntMap = BiMap.stringInt(data.items.keys)

    // collect Item as Map and convert ID to Int index
    val items: Map[Int, Item] = data.items.map { case (id, item) =>
      (itemStringIntMap(id), item)
    }.collectAsMap.toMap

    val itemCount = items.size

    val mllibRatings = data.viewEvents
      .map { r =>
        // Convert user and item String IDs to Int index for MLlib
        val uindex = userStringIntMap.getOrElse(r.user, -1)
        val iindex = itemStringIntMap.getOrElse(r.item, -1)

        if (uindex == -1)
          logger.info(s"Couldn't convert nonexistent user ID ${r.user}"
            + " to Int index.")

        if (iindex == -1)
          logger.info(s"Couldn't convert nonexistent item ID ${r.item}"
            + " to Int index.")

        ((uindex, iindex), 1)
      }.filter { case ((u, i), v) =>
        // keep events with valid user and item index
        (u != -1) && (i != -1)
      }.reduceByKey(_ + _) // aggregate all view events of same user-item pair
      .map { case ((u, i), v) =>
        // MLlibRating requires integer index for user and item
        MLlibRating(u, i, v)
      }

    val m = ALS.trainImplicit(mllibRatings, ap.rank, ap.numIterations)
    //val m = ALS.train(mllibRatings, ap.rank, ap.numIterations, ap.lambda)
    // productFeature is RDD[(Int, Array[Double])],
    val entries: RDD[MatrixEntry] = m.productFeatures
      .flatMap { case (i, features) =>
        features.zipWithIndex.map { case (v, r) => MatrixEntry(r, i, v) }
      }
    val corMat = new CoordinateMatrix(entries, m.rank, itemCount)
    // columnSimilarities returns is n x n sparse upper-triangular
    // coordinate matrix of cosine similarities between columns
    val half = corMat.toRowMatrix
      .columnSimilarities(ap.threshold)
      .entries

    val similarities: RDD[(Int, Vector[(Int, Double)])] = half
      .union(half.map(e => MatrixEntry(e.j, e.i, e.value)))
      .map(e => (e.i.toInt, (e.j.toInt, e.value)))
      .aggregateByKey(Vector[(Int, Double)]())(
        seqOp = ( (u, v) => v +: u ),
        combOp = ( (u1, u2) => u1 ++ u2))

    new ALSModel(
      similarities = similarities,
      itemStringIntMap = itemStringIntMap,
      items = items
    )
  }

  def predict(model: ALSModel, query: Query): PredictedResult = {
    // convert the white and black list items to Int index
    val whiteList: Option[Set[Int]] = query.whiteList.map( set =>
      set.map(model.itemStringIntMap.get(_)).flatten
    )
    val blackList: Option[Set[Int]] = query.blackList.map ( set =>
      set.map(model.itemStringIntMap.get(_)).flatten
    )

    val queryList: Set[Int] = query.items.map(model.itemStringIntMap.get(_))
      .flatten.toSet

    val indexScores = query.items.flatMap { iid =>
      model.itemStringIntMap.get(iid).map { itemInt =>
        val simsSeq = model.similarities.lookup(itemInt)
        if (simsSeq.isEmpty) {
          logger.info(s"No similar items found for ${iid}.")
          Vector[(Int, Double)]()
        } else {
          val sims = simsSeq.head
          sims.filter { case (i, v) =>
            whiteList.map(_.contains(i)).getOrElse(true) &&
            blackList.map(!_.contains(i)).getOrElse(true) &&
            // discard items in query as well
            (!queryList.contains(i)) &&
            // filter categories
            query.categories.map { cat =>
              model.items(i).categories.map { itemCat =>
                // keep this item if has ovelap categories with the query
                !(itemCat.toSet.intersect(cat).isEmpty)
              }.getOrElse(false) // discard this item if it has no categories
            }.getOrElse(true)
          }
        }
      }.getOrElse {
        logger.info(s"No similar items for unknown item ${iid}.")
        Vector[(Int, Double)]()
      }
    }

    val aggregatedScores = indexScores.groupBy(_._1)
      .mapValues(_.foldLeft[Double](0)( (b,a) => b + a._2))
      .toList

    val ord = Ordering.by[(Int, Double), Double](_._2).reverse
    val itemScores = getTopN(aggregatedScores, query.num)(ord)
      .map{ case (i, s) =>
        new ItemScore(
          item = model.itemIntStringMap(i),
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
