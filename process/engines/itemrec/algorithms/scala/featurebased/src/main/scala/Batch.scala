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

object UserProfileRecommendationBatch {
  val logger = Logger(UserProfileRecommendationBatch.getClass)
  val commonsConfig = new Config


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

  def main(cmdArgs: Array[String]) = {
    val args = Args(cmdArgs)

    val appid = args("appid").toInt
    val algoid = args("algoid").toInt
    val modelset = args("modelSet").toBoolean
    val numRecommendations = args.optional("numRecommendations")
    .getOrElse("10").toInt
    val verbose = args.optional("verbose").getOrElse("false").toBoolean
    val optWhiteItypesStr = args.optional("whiteItypes")

    // Recommendation 
    val (itypes, itemTypesMap) = UserProfileRecommendation.getItems(appid)

    val whiteItypes = UserProfileRecommendation.getWhiteItypes(
      itypes, optWhiteItypesStr)

    val whiteInvItypes = (0 until whiteItypes.length)
      .map(i => (whiteItypes(i), i)).toMap

    val userU2IsMap = UserProfileRecommendation.getU2I(appid)

    val userFeaturesMap = UserProfileRecommendation.constructUserFeatureMap(
      whiteInvItypes, itemTypesMap, userU2IsMap)

    val userRecommendationsMap = UserProfileRecommendation.recommend(
      userFeaturesMap, itemTypesMap, itypes, whiteInvItypes, 
      userFeaturesMap.keys.toSeq, numRecommendations)

    // Model Construction
    modelCon(appid, algoid, modelset, userRecommendationsMap, itemTypesMap)

    if (verbose) {
      UserProfileRecommendation.printRecommendations(
        userFeaturesMap, whiteItypes,
        userRecommendationsMap, itemTypesMap)
    }
  }
}
