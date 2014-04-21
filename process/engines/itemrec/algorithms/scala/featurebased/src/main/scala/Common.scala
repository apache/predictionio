package io.prediction.algorithms.itemrec.featurebased

import grizzled.slf4j.Logger
import io.prediction.commons.Config
import com.twitter.scalding.Args
import io.prediction.commons.appdata.U2IAction
import io.prediction.commons.modeldata.ItemRecScore

object UserProfileRecommendation {
  val logger = Logger(UserProfileRecommendation.getClass)
  val commonsConfig = new Config

  // distinct itypes
  // iid -> itypes
  def getItems(appid: Int): (Seq[String], Map[String, Seq[String]]) = {
    val itemsDb = commonsConfig.getAppdataItems

    // FIXME(yipjustin) filter by startT, endT.
    val itemTypesMap = itemsDb.getByAppid(appid)
    .map(item => (item.id, item.itypes)).toMap

    val itypes: Seq[String]  = itemTypesMap.values.toSeq.flatten.distinct
    
    return (itypes, itemTypesMap)
  }

  def getU2I(appid: Int): (Map[String, Seq[U2IAction]]) = {
    val u2iDb = commonsConfig.getAppdataU2IActions
    u2iDb.getAllByAppid(appid).toSeq.groupBy(_.uid)
  }

  def constructUserFeatureMap(
    invItypes: Map[String, Int],
    itemTypesMap: Map[String, Seq[String]],
    userU2IsMap: Map[String, Seq[U2IAction]]) : Map[String, Seq[Double]] = {
    
    val userFeatureMap = userU2IsMap.map{ case(user, u2is) => {
      val userFeature = u2is.map{ u2i => {
        val feature = new Array[Int](invItypes.size)
        itemTypesMap(u2i.iid).foreach(e => feature(invItypes(e)) = 1)
        feature
      }}.transpose.map(_.sum).toList
    
      val featureSum = userFeature.sum 
      val normalizedUserFeature = userFeature.map(_.toDouble / featureSum)
      //println(user + " : " + normalizedUserFeature) 
      (user, normalizedUserFeature.toList)
    }}.toMap
    userFeatureMap
  }

  def printFeature(feature: Seq[Double], itypes: Seq[String]): String = {
    (feature.zip(itypes)).filter(_._1 > 0)
    .map(e => f"${e._2}=${e._1}%.4f").reduce(_ + "," + _)
  }

  def recommend(
    userFeaturesMap: Map[String, Seq[Double]],
    itemTypesMap: Map[String, Seq[String]],
    allItypes: Seq[String],
    invItypes: Map[String, Int],
    users: Seq[String],
    numRecommendations: Int
  ) : Map[String, Seq[(String, Double)]] = {

    users.map { uid => {
      val userFeatures = userFeaturesMap(uid)
      
      val itemScoreMap = itemTypesMap.map{ case(iid, itypes) => {
        val score = itypes
        .map(itype => invItypes(itype))
        .map(idx => userFeatures(idx))
        .sum
        // FIXME: not decided yet. if item has too many types, need to discount
        // them
        // / itypes.size  
        (iid, score)
      }}

      val top = itemScoreMap.toList.sortBy(-_._2).take(numRecommendations)
      (uid, top)
    }}.toMap
  }

  def printRecommendations(
    userFeaturesMap: Map[String, Seq[Double]],
    itypes: Seq[String],
    userRecommendationsMap: Map[String, Seq[(String, Double)]],
    itemTypesMap: Map[String, Seq[String]]) {
    userRecommendationsMap.foreach{ case(user, recommendations) => {
      println("User: " + user)
      println(" Features: " + 
        printFeature(userFeaturesMap(user), itypes))
      recommendations.foreach{ case (iid, score) => {
        println(" Item: " + iid + f" score: $score%4f F: " + itemTypesMap(iid))
      }}
      println
    }}
  }
}

