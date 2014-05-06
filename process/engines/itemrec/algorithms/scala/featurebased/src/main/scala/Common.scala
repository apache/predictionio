package io.prediction.algorithms.itemrec.featurebased

import grizzled.slf4j.Logger
import io.prediction.commons.Config
import com.twitter.scalding.Args
import io.prediction.commons.appdata.{ Item, User, U2IAction }
import io.prediction.commons.modeldata.ItemRecScore

class UserProfileRecommendationException(msg: String = null, cause: Throwable=null) 
extends RuntimeException(msg, cause)


// Only consider items rated >= 3
object UserProfileRecommendation {
  val logger = Logger(UserProfileRecommendation.getClass)
  val commonsConfig = new Config

  // Return itypes if whiteItypesStr is empty or opt is None
  // If WhiteItypesStr is specify, return their intersection, using whiteItypes
  // order.
  def getFeatureItypes(itypes: Seq[String], optFeatureItypesStr: Option[String])
  : Seq[String] = {
    if (optFeatureItypesStr.isEmpty)
      return itypes

    val featureItypesStr = optFeatureItypesStr.get
    if (featureItypesStr == "")
      return itypes

    val itypeSet = itypes.toSet
    featureItypesStr.split(',').filter{
      featureItype => itypeSet.contains(featureItype)
    }
  }

  def getUsers(appid: Int): Seq[User] = {
    val usersDb = commonsConfig.getAppdataUsers
    usersDb.getByAppid(appid).toSeq
  }

  // distinct itypes
  // iid -> itypes
  // [white iid]
  def getItems(appid: Int, whiteItypes: Seq[String] = Seq[String]()): (
    Seq[String], Map[String, Seq[String]], Seq[String]) = {
    val itemsDb = commonsConfig.getAppdataItems

    val trainingItems = (if (whiteItypes.length == 0) itemsDb.getByAppid(appid)
      else itemsDb.getByAppidAndItypes(appid, whiteItypes)).toSeq

    val itemTypesMap = trainingItems.map(item => (item.id, item.itypes)).toMap

    val itypes: Seq[String]  = itemTypesMap.values.toSeq.flatten.distinct

    // create white item list, filter inactive items. Note that it is
    // independent of the whiteItypes.
    val whiteItems = itemsDb.getByAppid(appid)
      .filter(!_.inactive.getOrElse(false)).map(_.id).toSeq
    
    return (itypes, itemTypesMap, whiteItems)
  }

  def getU2I(appid: Int): (Map[String, Seq[U2IAction]]) = {
    val u2iDb = commonsConfig.getAppdataU2IActions
    u2iDb.getAllByAppid(appid).toSeq.groupBy(_.uid)
  }

  // Only featureInvItypes is used.
  def constructUserFeatureMap(
    featureInvItypes: Map[String, Int],
    itemTypesMap: Map[String, Seq[String]],
    uidList: Seq[String],
    userU2IsMap: Map[String, Seq[U2IAction]]) : Map[String, Seq[Double]] = {
    val userFeatureMap = uidList.map{ user => {
      val u2is = userU2IsMap.getOrElse(user, Seq[U2IAction]())

      // TODO. Discount early actions
      val userFeatureList = u2is
      .filter(_.action == "rate")
      .filter(_.v.getOrElse(0) > 3)
      .filter(u2i => itemTypesMap.contains(u2i.iid))
      .map{ u2i => {
        // Only populate the featurelisted itypes
        val feature = Array.fill[Int](featureInvItypes.size)(0)
        itemTypesMap(u2i.iid)
          .filter(featureInvItypes.contains)
          .foreach(e => feature(featureInvItypes(e)) = 1)

        feature
      }}

      val userFeature = (
        if (userFeatureList.length > 0) {
          val sumUserFeature = userFeatureList.transpose.map(_.sum).toList
          if (sumUserFeature.sum == 0) {
            // This happens when all items rated by the user have no interested
            // itypes. In such case we have to fill null.
            Seq.fill(featureInvItypes.size)(1)
          } else {
            sumUserFeature
          }
        } else {
          // For user has no feature, assumes uniform.
          Seq.fill(featureInvItypes.size)(1)
        }
      )
      
      val featureSum = userFeature.sum 
      val normalizedUserFeature = userFeature.map(_.toDouble / featureSum)

      (user, normalizedUserFeature.toList)
    }}.toMap
    userFeatureMap
  }

  // Notice that the third return value is item to *all* its itypes. It is
  // important to pass non-feature itypes since we need these info in modeldata
  // for other pruning.
  def constructUserFeaturesMapFromArg(
    appid: Int,
    optFeatureItypesStr: Option[String] = None,
    whiteItypes: Seq[String] = Seq[String]()) : (
    Map[String, Seq[Double]],  // User -> Itype Scores
    Seq[String],  // Itype Feature List
    Map[String, Seq[String]],  // Item -> All Itypes
    Seq[String]  // Whitelist of items can be passed to output
  ) = { 
    val (itypes, itemTypesMap, whiteItems) = getItems(appid, whiteItypes)
    
    val users = getUsers(appid).map(_.id)

    val featureItypes = getFeatureItypes(itypes, optFeatureItypesStr)
    if (featureItypes.length == 0) {
      throw new UserProfileRecommendationException("No items has featurelisted types")
    }

    val featureInvItypes = (0 until featureItypes.length)
      .map(i => (featureItypes(i), i)).toMap

    val userU2IsMap = getU2I(appid)

    val userFeaturesMap = constructUserFeatureMap(
      featureInvItypes, itemTypesMap, users, userU2IsMap)
     
    return (userFeaturesMap, featureItypes, itemTypesMap, whiteItems)
  }

  def printFeature(feature: Seq[Double], itypes: Seq[String]): String = {
    (feature.zip(itypes)).filter(_._1 > 0)
    .map(e => f"${e._2}=${e._1}%.4f").reduce(_ + "," + _)
  }

  def recommend(
    userFeaturesMap: Map[String, Seq[Double]],
    featureItypes: Seq[String],
    itemItypes: Map[String, Seq[String]],
    whiteItems: Seq[String],
    numRecommendations: Int
  ) : Map[String, Seq[(String, Double)]] = {

    val featureIdxMap = featureItypes.zip(0 until featureItypes.size).toMap

    userFeaturesMap.map { case(uid, features) => {
      val itemScoreMap = whiteItems
      .filter(iid => itemItypes.contains(iid))  // model must have this data.
      .map{ case(iid) => {
        val itypes = itemItypes(iid)
        val score = itypes
          .filter(featureIdxMap.contains)
          .map(itype => featureIdxMap(itype))
          .map(idx => features(idx))
          .sum
        // FIXME: not decided yet. if item has too many types, need to discount
        // them / itypes.size  
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

