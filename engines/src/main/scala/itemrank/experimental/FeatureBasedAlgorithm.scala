/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.engines.itemrank.experimental

import io.prediction.controller._

import io.prediction.engines.base.PreparedData
import io.prediction.engines.base.RatingTD

import io.prediction.engines.itemrank._

import nak.classify.NaiveBayes
import breeze.linalg.Counter

import nak.data.Example

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

// FIXME. For now, we don't save the model.
case class FeatureBasedModel(
  val features: Array[String] = Array[String](),
  val userClassifierMap: Map[String, NaiveBayes[Boolean, String]] =
    Map[String, NaiveBayes[Boolean, String]](),
  val itemFeaturesMap: Map[String, Counter[String, Double]] =
    Map[String, Counter[String, Double]]())
extends Serializable {
  override def toString = "FeatureBasedModel: " +
    s"features (size = ${features.size}) = [${features.take(3).toSeq}, ...], " +
    s"userClassifierMap (size = ${userClassifierMap.size}) " +
    s"= {${userClassifierMap.take(3).toSeq}, ...}, " +
    s"itemFeaturesMap (size = ${itemFeaturesMap.size}) " +
    s"= {${itemFeaturesMap.take(3).toSeq}, ...}"
}

// FeatureBaseAlgorithm use all itypes as features.
class FeatureBasedAlgorithm
  extends LAlgorithm[PreparedData, FeatureBasedModel,
      Query, Prediction] {

  def train(data: PreparedData): FeatureBasedModel = {
    val featureCounts = data.items
      .flatMap{ case(iindex, item) => item.itypes }
      .groupBy(identity)
      .mapValues(_.size)

    val features: Seq[String] = featureCounts.toSeq.sortBy(-_._2).map(_._1)

    // one model/classifier for each user in Naive Bayes
    val itemsSet = data.items.keySet
    // Map from uid to iid that user bought
    val conversionsMap: Map[Int, Set[Int]] = data.rating.groupBy(_.uindex)
      .mapValues(_.map(_.iindex).toSet)

    // iindex to feature counter map
    val itemFeaturesMap: Map[String, Counter[String, Double]] =
    data.items.map { case(iindex, item) => {
      val features = Counter[String, Double]()
      for (itype <- item.itypes) {
        features(itype) = 1.0
      }
      (data.items(iindex).iid, features)
    }}
    .toMap

    val trainer = new NaiveBayes.Trainer[Boolean, String]

    val userClassifierMap: Map[String, NaiveBayes[Boolean, String]] =
    conversionsMap.map { case (uindex, iindicies) => {

      // Construct the iterable for training a model for this user
      val positiveExamples: Seq[Example[Boolean, Counter[String, Double]]] =
        iindicies.map { iindex => Example(label=true, features=itemFeaturesMap(data.items(iindex).iid)) }.toSeq
      val negativeExamples: Seq[Example[Boolean, Counter[String, Double]]] =
        (itemsSet -- iindicies).filter(_ % 101 == uindex % 101).map { iindex => Example(label=false, features=itemFeaturesMap(data.items(iindex).iid)) }.toSeq
      val examples = positiveExamples ++ negativeExamples

      // build the model
      (data.users(uindex).uid, trainer.train(examples.toIterable))
    }}
    .toMap

    FeatureBasedModel(
      featureCounts.keys.toArray,
      userClassifierMap,
      itemFeaturesMap
    )
  }

  def predict(model: FeatureBasedModel, query: Query): Prediction = {
    val (items, isOriginal): (Seq[(String, Double)], Boolean) = (
      if (model.userClassifierMap.contains(query.uid)) {
        val items: Seq[(String, Double)] = query.iids
        .map { iid => {
          if (model.itemFeaturesMap.contains(iid)) {
            (iid, model.userClassifierMap(query.uid).scores(model.itemFeaturesMap(iid))(true))
          } else {
            (iid, 0.0) // item not found
          }
        }}
        .sortBy(-_._2)
        (items, false)
      } else {
        // if user not found, use input order.
        (query.iids.map { iid => (iid, 0.0) }, true)
      }
    )
    new Prediction(items, isOriginal)
  }
}
