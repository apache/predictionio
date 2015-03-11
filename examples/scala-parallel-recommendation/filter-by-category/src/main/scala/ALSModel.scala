package org.apache.spark.mllib.recommendation
// This must be the same package as Spark's MatrixFactorizationModel because
// MatrixFactorizationModel's constructor is private and we are using
// its constructor in order to save and load the model

import org.template.recommendation.ALSAlgorithmParams

import io.prediction.controller.IPersistentModel
import io.prediction.controller.IPersistentModelLoader
import io.prediction.data.storage.BiMap

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

class ALSModel(
    override val rank: Int,
    override val userFeatures: RDD[(Int, Array[Double])],
    override val productFeatures: RDD[(Int, Array[Double])])
  extends MatrixFactorizationModel(rank, userFeatures, productFeatures)
  with IPersistentModel[ALSAlgorithmParams] {

  def save(id: String, params: ALSAlgorithmParams,
    sc: SparkContext): Boolean = {

    userFeatures.saveAsObjectFile(s"/tmp/${id}/userFeatures")
    productFeatures.saveAsObjectFile(s"/tmp/${id}/productFeatures")
    true
  }

  override def toString = {
    s"userFeatures: [${userFeatures.count()}]" +
    s"(${userFeatures.take(2).toList}...)" +
    s" productFeatures: [${productFeatures.count()}]" +
    s"(${productFeatures.take(2).toList}...)"
  }
}

object ALSModel
  extends IPersistentModelLoader[ALSAlgorithmParams, ALSModel] {
  def apply(id: String, params: ALSAlgorithmParams, sc: Option[SparkContext]) = {
    new ALSModel(
      rank = params.rank,
      userFeatures = sc.get.objectFile(s"/tmp/${id}/userFeatures"),
      productFeatures = sc.get.objectFile(s"/tmp/${id}/productFeatures"))
  }
}

class CategoriesALSModels(
    val modelsMap: Map[String, ALSModel],
    val userStringIntMap: BiMap[String, Int],
    val itemStringIntMap: BiMap[String, Int])
  extends IPersistentModel[ALSAlgorithmParams] {
  def save(id: String, params: ALSAlgorithmParams, sc: SparkContext): Boolean = {
    sc.parallelize(modelsMap.keys.toSeq)
      .saveAsObjectFile(s"/tmp/${id}/mapKeys")
    sc.parallelize(Seq(userStringIntMap))
      .saveAsObjectFile(s"/tmp/${id}/userStringIntMap")
    sc.parallelize(Seq(itemStringIntMap))
      .saveAsObjectFile(s"/tmp/${id}/itemStringIntMap")

    modelsMap.foreach {
      case (category, model) =>
        model.save(s"${id}-${category}", params, sc)
    }
    true
  }

  override def toString = {
    s"categories: [${modelsMap.size}]" +
    s"(${modelsMap.keys.take(2)}...})" +
    s" userStringIntMap: [${userStringIntMap.size}]" +
    s"(${userStringIntMap.take(2)}...)" +
    s" itemStringIntMap: [${itemStringIntMap.size}]" +
    s"(${itemStringIntMap.take(2)}...)"
  }
}

object CategoriesALSModels
  extends IPersistentModelLoader[ALSAlgorithmParams, CategoriesALSModels] {
  def apply(id: String, params: ALSAlgorithmParams, sc: Option[SparkContext]) = {
    val keys = sc.get
      .objectFile[String](s"/tmp/${id}/mapKeys").collect().toList

    val modelMap = keys.map { category =>
      category -> ALSModel.apply(s"${id}-${category}", params, sc)
    }.toMap

    val userStringIntMap = sc.get
      .objectFile[BiMap[String, Int]](s"/tmp/${id}/userStringIntMap").first
    val itemStringIntMap = sc.get
      .objectFile[BiMap[String, Int]](s"/tmp/${id}/itemStringIntMap").first

    new CategoriesALSModels(modelMap, userStringIntMap, itemStringIntMap)
  }
}
