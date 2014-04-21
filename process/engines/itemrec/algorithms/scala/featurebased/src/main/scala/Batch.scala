package io.prediction.algorithms.itemrec.featurebased

import grizzled.slf4j.Logger
import io.prediction.commons.Config
import com.twitter.scalding.Args
import io.prediction.commons.appdata.U2IAction
import io.prediction.commons.modeldata.ItemRecScore

/*
 * - construct feature-list, Seq[String]
 * - construct user-feature-map : Map[User, Seq[Double]]
 * - recommend: Map[User, Seq[(Item, Double)]]
 * - modelCon: output ItemRec in datastore.
 */

object FeatureBasedUserProfileRecommendation {
  val logger = Logger(FeatureBasedUserProfileRecommendation.getClass)
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

  def modelCon(appid: Int, algoid: Int, modelset: Boolean,
    userRecommendationsMap: Map[String, Seq[(String, Double)]],
    itemTypesMap: Map[String, Seq[String]]
  ) = {
    val modeldataDb = commonsConfig.getModeldataItemRecScores

    userRecommendationsMap.foreach{ case(user, recommendations) => {
      modeldataDb.insert(ItemRecScore(
        uid = user,
        iids = recommendations.map(_._1),
        scores = recommendations.map(_._2),
        itypes = recommendations.map{ e => itemTypesMap(e._1) },
        appid = appid,
        algoid = algoid,
        modelset = modelset))
    }}
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


  def main(cmdArgs: Array[String]) = {
    val args = Args(cmdArgs)

    val appid = args("appid").toInt
    val algoid = args("algoid").toInt
    val modelset = args("modelSet").toBoolean
    val numRecommendations = args.optional("numRecommendations")
    .getOrElse("10").toInt
    val verbose = args.optional("verbose").getOrElse("false").toBoolean

    val (itypes, itemTypesMap) = getItems(appid)
    val invItypes = (0 until itypes.length).map(i => (itypes(i), i)).toMap

    val userU2IsMap = getU2I(appid)

    val cStart = System.currentTimeMillis
    val userFeaturesMap = constructUserFeatureMap(
      invItypes, itemTypesMap, userU2IsMap)
    val cEnd = System.currentTimeMillis

    val userRecommendationsMap = recommend(
      userFeaturesMap, itemTypesMap, itypes, invItypes, 
      userFeaturesMap.keys.toSeq, numRecommendations)

    modelCon(appid, algoid, modelset,
      userRecommendationsMap, itemTypesMap)

    if (verbose) {
      printRecommendations(userFeaturesMap, itypes,
        userRecommendationsMap, itemTypesMap)
    }
  }
}
