package io.prediction.workflow

import io.prediction.core.BaseDataSource
import io.prediction.core.BasePreparator
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseServing
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import scala.language.implicitConversions

import grizzled.slf4j.{ Logger, Logging }

object EngineWorkflow {
  type EX = Int
  type AX = Int
  type QX = Long

  @transient lazy val logger = Logger[this.type]

  def train[TD, PD, Q](
      sc: SparkContext,
      dataSource: BaseDataSource[TD, _, Q, _],
      preparator: BasePreparator[TD, PD],
      algorithmList: Seq[BaseAlgorithm[PD, _, Q, _]]): Seq[Any] = {
    logger.info("EngineWorkflow.train")
    logger.info(s"DataSource: $dataSource")
    logger.info(s"Preparator: $preparator")
    logger.info(s"AlgorithmList: $algorithmList")

    val td = dataSource.readTrainingBase(sc)
    val pd = preparator.prepareBase(sc, td)
    val models: Seq[Any] = algorithmList.map(_.trainBase(sc, pd))
    logger.info("EngineWorkflow.train completed")
    models
  }

  def eval[TD, PD, Q, P, A, EI](
      sc: SparkContext,
      dataSource: BaseDataSource[TD, EI, Q, A],
      preparator: BasePreparator[TD, PD],
      algorithmList: Seq[BaseAlgorithm[PD, _, Q, P]],
      serving: BaseServing[Q, P]): Seq[(EI, RDD[(Q, P, A)])] = {
    logger.info(s"DataSource: $dataSource")
    logger.info(s"Preparator: $preparator")
    logger.info(s"AlgorithmList: $algorithmList")
    logger.info(s"Serving: $serving")

    val algoMap: Map[AX, BaseAlgorithm[PD, _, Q, P]] = algorithmList
      .zipWithIndex
      .map(_.swap)
      .toMap
    val algoCount = algoMap.size

    val evalTupleMap: Map[EX, (TD, EI, RDD[(Q, A)])] = dataSource
      .readEvalBase(sc)
      .zipWithIndex
      .map(_.swap)
      .toMap

    val evalCount = evalTupleMap.size

    val evalTrainMap: Map[EX, TD] = evalTupleMap.mapValues(_._1)
    val evalInfoMap: Map[EX, EI] = evalTupleMap.mapValues(_._2)
    val evalQAsMap: Map[EX, RDD[(QX, (Q, A))]] = evalTupleMap
      .mapValues(_._3)
      .mapValues{ _.zipWithUniqueId().map(_.swap) }

    val preparedMap: Map[EX, PD] = evalTrainMap.mapValues { td => {
      preparator.prepareBase(sc, td)
    }}

    //val algoModelsMap: Map[EX, Seq[Any]] = preparedMap.mapValues { pd => {
    val algoModelsMap: Map[EX, Map[AX, Any]] = preparedMap.mapValues { pd => {
      //algorithmList.map(_.prepareBase(sc, pd))
      algoMap.mapValues(_.trainBase(sc,pd))
    }}

    //val algoPredictsMap: Map[EX, Seq[RDD[(QX, P)]]] = (0 until evalCount)
    val algoPredictsMap: Map[EX, RDD[(QX, Seq[P])]] = (0 until evalCount)
    .map { ex => {
      val modelMap: Map[AX, Any] = algoModelsMap(ex)

      val qs: RDD[(QX, Q)] = evalQAsMap(ex).mapValues(_._1)

      //val algoPredicts: Seq[RDD[(QX, A)]] = (0 until 
      val algoPredicts: Seq[RDD[(QX, (AX, P))]] = (0 until algoCount)
      .map { ax => {
        val algo = algoMap(ax)
        val model = modelMap(ax)
        val rawPredicts: RDD[(QX, P)] = algo.batchPredictBase(sc, model, qs)
        //val predicts: RDD[(QX, (AX, P))] = rawPredicts.mapValues { p => (ax, p) }
        val predicts: RDD[(QX, (AX, P))] = rawPredicts.map { case (qx, p) => {
          (qx, (ax, p))
        }}
        predicts
      }}

      //val unionAlgoPredicts: RDD[(QX, (Seq[P], (Q, A)))] = 
      val unionAlgoPredicts: RDD[(QX, Seq[P])] = sc.union(algoPredicts)
      .groupByKey
      .mapValues { ps => {
        assert (ps.size == algoCount, "Must have same length as algoCount") 
        // TODO. Check size == algoCount
        ps.toSeq.sortBy(_._1).map(_._2) 
      }}

      (ex, unionAlgoPredicts)
    }}
    .toMap

    val servingQPAMap: Map[EX, RDD[(Q, P, A)]] = algoPredictsMap
    .map { case (ex, psMap) => {
      val qasMap: RDD[(QX, (Q, A))] = evalQAsMap(ex)
      val qpsaMap: RDD[(QX, Q, Seq[P], A)] = psMap.join(qasMap)
      .map { case (qx, t) => (qx, t._2._1, t._1, t._2._2) }

      val qpaMap: RDD[(Q, P, A)] = qpsaMap.map { 
        case (qx, q, ps, a) => (q, serving.serveBase(q, ps), a) 
      }
      (ex, qpaMap)
    }}

    (0 until evalCount).map { ex => {
      (evalInfoMap(ex), servingQPAMap(ex))
    }}
    .toSeq
  }


}

