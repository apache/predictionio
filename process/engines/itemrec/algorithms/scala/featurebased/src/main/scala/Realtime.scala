package io.prediction.algorithms.itemrec.featurebased

import grizzled.slf4j.Logger
import io.prediction.commons.Config
import com.twitter.scalding.Args
import io.prediction.commons.appdata.U2IAction
import io.prediction.commons.modeldata.ItemRecScore
import io.prediction.commons.modeldata.{ MetadataKeyvals, MetadataKeyval }

/*
 * - construct feature-list, Seq[String]
 * - construct user-feature-map : Map[User, Seq[Double]]
 * - modelCon: output UserFeatureMap in datastore.
 */

object FeatureBasedUserProfileRecommendationRealtime {
  val logger = Logger(FeatureBasedUserProfileRecommendationBatch.getClass)
  val commonsConfig = new Config

  def modelCon(appid: Int, algoid: Int, modelset: Boolean,
    itypes: Seq[String],
    userFeaturesMap: Map[String, Seq[Double]]) {
    // Write to metadata-keyval
    val keyvalDb = commonsConfig.getModeldataMetadataKeyvals
    keyvalDb.upsert(algoid, modelset, "features", itypes.reduce(_ + "," + _))

    val userFeaturesDb = commonsConfig.getModeldataItemRecScores
    userFeaturesMap.foreach { case(user, features) => {
      userFeaturesDb.insert(ItemRecScore(
        uid = user,
        iids = Seq[String](),
        scores = features,
        itypes = Seq[Seq[String]](),
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
    //val numRecommendations = args.optional("numRecommendations")
    //.getOrElse("10").toInt
    val verbose = args.optional("verbose").getOrElse("false").toBoolean

    val (itypes, itemTypesMap) = UserProfileRecommendation.getItems(appid)
    val invItypes = (0 until itypes.length).map(i => (itypes(i), i)).toMap

    val userU2IsMap = UserProfileRecommendation.getU2I(appid)

    val userFeaturesMap = UserProfileRecommendation.constructUserFeatureMap(
      invItypes, itemTypesMap, userU2IsMap)

    modelCon(appid, algoid, modelset, itypes, userFeaturesMap)

    /*
    val userRecommendationsMap = UserProfileRecommendation.recommend(
      userFeaturesMap, itemTypesMap, itypes, invItypes, 
      userFeaturesMap.keys.toSeq, numRecommendations)

    modelCon(appid, algoid, modelset,
      userRecommendationsMap, itemTypesMap)
    */

    /*
    if (verbose) {
      UserProfileRecommendation.printRecommendations(userFeaturesMap, itypes,
        userFeaturesMap, itemTypesMap)
    }
    */
  }

}
