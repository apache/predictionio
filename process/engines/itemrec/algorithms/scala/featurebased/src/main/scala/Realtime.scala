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

object UserProfileRecommendationRealtime {
  val logger = Logger(UserProfileRecommendationRealtime.getClass)
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
    val verbose = args.optional("verbose").getOrElse("false").toBoolean
    val optWhiteItypesStr = args.optional("whiteItypes")

    val (itypes, itemTypesMap) = UserProfileRecommendation.getItems(appid)
    //val invItypes = (0 until itypes.length).map(i => (itypes(i), i)).toMap
    val whiteItypes = UserProfileRecommendation.getWhiteItypes(
      itypes, optWhiteItypesStr)

    val whiteInvItypes = (0 until whiteItypes.length)
      .map(i => (whiteItypes(i), i)).toMap

    val userU2IsMap = UserProfileRecommendation.getU2I(appid)

    val userFeaturesMap = UserProfileRecommendation.constructUserFeatureMap(
      whiteInvItypes, itemTypesMap, userU2IsMap)
      //invItypes, itemTypesMap, userU2IsMap)

    //modelCon(appid, algoid, modelset, itypes, userFeaturesMap)
    modelCon(appid, algoid, modelset, whiteItypes, userFeaturesMap)
  }

}
