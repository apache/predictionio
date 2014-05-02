package io.prediction.algorithms.itemrec.featurebased 

import org.specs2.mutable._
import org.specs2.specification.Step
import org.specs2.matcher.{ Matcher, Expectable }

import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

import io.prediction.commons.Config
import com.mongodb.casbah.Imports._
import io.prediction.commons.appdata.{ Item, User, U2IAction }
import io.prediction.commons.settings.{ App, Algo }

object CustomMatcher {
  def matchSeqDouble(expected: Seq[Double], epsilon: Double = 0.0001):
  Matcher[Seq[Double]] = new Matcher[Seq[Double]] {
    def apply[S <: Seq[Double]](actual: Expectable[S]) = {
      val equalLength = (actual.value.length == expected.length)
      val elementEqual = actual.value.zip(expected)
        .map(e => ((e._1 - e._2).abs <= epsilon))
        .reduce(_ && _)

      result(equalLength && elementEqual,
        s"Two Seq[Double] are almost equal (epsilon = $epsilon)",
        s"Two Seq[Double] not eq: Length: $equalLength element: $elementEqual. " + 
        s"Expected: $expected; Actual: ${actual.value}",
        actual)
    }
  }

  // actual must be same as expected
  def matchMapStringDouble(expected: Map[String, Double], 
  epsilon: Double = 0.0001): Matcher[Map[String, Double]] =
  new Matcher[Map[String, Double]] {

    def apply[S <: Map[String, Double]](actual: Expectable[S]) = {
      val equalLength = (actual.value.size == expected.size)
    
      val elementEqual = actual.value.map{ case(k, v) => {
        // true only if two numbers can be found and are within epsilon
        expected.get(k).map(e => (e - v).abs <= epsilon).getOrElse(false)
      }}.reduce(_ && _)

      result(equalLength && elementEqual,
        s"Two Map[S, Double] are almost equal (epsilon = $epsilon)",
        s"Two Map[S, Double] not eq: Length: $equalLength element: $elementEqual. " + 
        s"Expected: $expected; Actual: ${actual.value}",
        actual)
    }
  }
 
  // Actual must contain all pairs in expected
  def containMapStringDouble(expected: Map[String, Double], 
  epsilon: Double = 0.0001): Matcher[Map[String, Double]] =
  new Matcher[Map[String, Double]] {

    def apply[S <: Map[String, Double]](actual: Expectable[S]) = {
      val containElement = expected.map{ case(k, v) => {
        // true only if two numbers can be found and are within epsilon
        actual.value.get(k).map(e => (e - v).abs <= epsilon).getOrElse(false)
      }}.reduce(_ && _)

      result(containElement,
        s"Actual contains expected",
        s"Actual doesn't contain expected" + 
        s"Expected: $expected; Actual: ${actual.value}",
        actual)
    }
  }
}

class CustomMatcherSpec extends Specification {
  import io.prediction.algorithms.itemrec.featurebased.CustomMatcher._

  "MatchSeqDouble" should {
    "false unequal length" in {
      (Seq(1.0, 2.0, 3.0, 4.0) must matchSeqDouble(Seq(1.00001, 2.0, 2.99999)) not)
    }
    "true within epsilon" in {
      (Seq(1.0, 2.0, 3.0) must matchSeqDouble(Seq(1.00001, 2.0, 2.99999)))
    }
    "false outside epsilon" in {
      (Seq(1.0, 2.0, 3.0) must
        matchSeqDouble(
          Seq(1.00001, 2.0, 2.99999), epsilon=0.000001) not)
    }
  }
  
  "MatchMapStringDouble" should {
    "true within epsilon" in {
      val actual = Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0)
      val expected = Map("a" -> 0.999999, "b" -> 2.0, "c" -> 3.000001)
      actual must matchMapStringDouble(expected)
    }
    
    "false outside epsilon" in {
      val actual = Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0)
      val expected = Map("a" -> 0.999999, "b" -> 2.0, "c" -> 3.000001)
      (actual must matchMapStringDouble(expected, epsilon=0.0000001) not)
    }
    
    "false mismatched items" in {
      val actual = Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0)
      val expected = Map("a" -> 0.999999, "b" -> 2.0, "d" -> 3.0)
      (actual must matchMapStringDouble(expected) not)
    }
  }

  // FIXME(yipjustin): Add spec for ContainMap
}


class FeatureBasedItemRecSpec extends Specification {
  import io.prediction.algorithms.itemrec.featurebased.CustomMatcher._

  //val excludePattern = "CustomMatcher"
  /*
  val excludePattern = "io\\.prediction\\.algorithms\\.itemrec\\.featurebased"

  override def is = args.report(traceFilter = includeAlsoTrace(excludePattern)) ^
  super.is
  */
  //args.report(traceFilter=excludeAlsoTrace(excludePattern))

  def cleanUp() = {
    val connection = MongoConnection()
    Seq(
      "predictionio_appdata_scala_itemrec_featurebased_test",
      "predictionio_modeldata_scala_itemrec_featurebased_test"
      ).foreach( mongoDbName => connection(mongoDbName).dropDatabase() )
  }

  val commonConfig = new Config
  val appdataUsers = commonConfig.getAppdataUsers
  val appdataItems = commonConfig.getAppdataItems
  val appdataU2IActions = commonConfig.getAppdataU2IActions

  val appid = 42
    
  val anotherAppid = 9527

  val rawItems = Map(
    "i1" -> Seq("t1", "t2"),
    "i2" -> Seq[String](), 
    "i3" -> Seq("t1", "t3"),
    "i4" -> Seq("t1", "t2", "t3"),
    "i5" -> Seq("t4"),
    "i6" -> Seq("t2", "t3"),
    "i7" -> Seq("t2", "t4"))
  val rawUsers = Seq("u1", "u2", "u3", "u4")
  val rawU2Is = Map(
    "u1" -> Seq(
      ("i1", "rate", 4),
      ("i1", "rate", 5),
      ("i3", "rate", 4),
      ("i5", "rate", 3)),
    "u2" -> Seq(
      ("i1", "view", 1),
      ("i5", "rate", 4),
      ("i0", "rate", 5)),  // non-exist item
    "u3" -> Seq(
      ("i2", "rate", 5),
      ("i2", "rate", 3)),  // no itypes
    "u0" -> Seq(
      ("i1", "rate", 4),
      ("i3", "rate", 5)))

  rawUsers.foreach{ uid => appdataUsers.insert(User(
    id = uid,
    appid = appid,
    ct = DateTime.now))}
    
  rawItems.foreach{ case(iid, itypes) => {
    appdataItems.insert(Item(
      id = iid,
      appid = appid,
      ct = DateTime.now,
      itypes = itypes,
      starttime = None,
      endtime = None))
  }}

  rawU2Is.foreach{ case(uid, actions) => {
    actions.foreach { action => appdataU2IActions.insert(U2IAction(
      appid = appid,
      action = action._2,
      uid = uid,
      iid = action._1,
      t = DateTime.now,
      v = Some(action._3))) }
  }}
    
  def getApp(appid: Int) = App(
    id = appid,
    userid = 0,
    appkey = "123",
    display = "12345")

  def getAlgo(algoid: Int, modelset: Boolean) = Algo(
    id = algoid,
    engineid = 1234,
    name = "",
    infoid = "abc",
    command = "",
    modelset = modelset,
    createtime = DateTime.now,
    updatetime = DateTime.now)


  "Extract correct itypes"  should {
    val input = Seq("a", "b", "c")
    "No feature itypes" in {
      val r = UserProfileRecommendation.getFeatureItypes(input, None)
      input === r
    }
    "Empty feature itypes" in {
      val r = UserProfileRecommendation.getFeatureItypes(input, Some(""))
      input === r
    }
    "Support feature itypes, same as input feature sequence" in {
      val r = UserProfileRecommendation.getFeatureItypes(input, Some("c,b,d"))
      r === Seq("c", "b")
    }
  }

  "Get items and itypes" should {
    "Empty App" in {
      val r = UserProfileRecommendation.getItems(1679)
      r._1 must have size(0)
      r._2 must have size(0)
    }

    "Default App" in {
      val r = UserProfileRecommendation.getItems(appid)
      val itypes = r._1
      itypes must containTheSameElementsAs(Seq("t4", "t3", "t2", "t1"))
    }
  }

  "Construct user features map" should {
    "Run with all itypes" in {
      val (userFeaturesMap, featureItypes, itemTypesMap) = (
        UserProfileRecommendation.constructUserFeaturesMapFromArg(
          appid, Some("t4,t1,t2,t3")))

      val expectedUserFeaturesMap = Map(
        "u1" -> Seq(0.0, 0.5, 0.333333, 0.16666),
        "u2" -> Seq(1.0, 0.0, 0.0, 0.0),
        "u3" -> Seq(0.25, 0.25, 0.25, 0.25),
        "u4" -> Seq(0.25, 0.25, 0.25, 0.25))

      expectedUserFeaturesMap.map{ case(user, features) => {
        userFeaturesMap(user) must CustomMatcher.matchSeqDouble(features)
      }}.reduce(_ and _)

      userFeaturesMap.keys must containTheSameElementsAs(
        expectedUserFeaturesMap.keys.toSeq)
    }
    
    "Run with feature itypes t1,t2" in {
      val (userFeaturesMap, featureItypes, itemTypesMap) = (
        UserProfileRecommendation.constructUserFeaturesMapFromArg(
          appid, Some("t1,t2")))

      val expectedUserFeaturesMap = Map(
        "u1" -> Seq(0.6, 0.4),
        "u2" -> Seq(0.5, 0.5),
        "u3" -> Seq(0.5, 0.5),
        "u4" -> Seq(0.5, 0.5))

      expectedUserFeaturesMap.map{ case(user, features) => {
        userFeaturesMap(user) must CustomMatcher.matchSeqDouble(features)
      }}.reduce(_ and _)

      userFeaturesMap.keys must containTheSameElementsAs(
        expectedUserFeaturesMap.keys.toSeq)

      featureItypes must containTheSameElementsAs(Seq("t1", "t2"))
    }
    
    "Run failure with feature (but not exist) itypes t6,t7" in {
      UserProfileRecommendation.constructUserFeaturesMapFromArg(
          appid, Some("t6,t7")) must throwA[UserProfileRecommendationException]
    }
    
    "Run failure with empty app" in {
      UserProfileRecommendation.constructUserFeaturesMapFromArg(
          anotherAppid, Some("")) must throwA[UserProfileRecommendationException]
    }
  }

  "Construct batch recommendation" should {
    "Run with all itypes" in {
      val (userFeaturesMap, featureItypes, itemItypesMap) = (
        UserProfileRecommendation.constructUserFeaturesMapFromArg(
          appid, Some("")))
      val userRecommendationMap = UserProfileRecommendation.recommend(
        userFeaturesMap, featureItypes, itemItypesMap, 100)
     
      /*
      println("f")
      println(featureItypes)
      println("u->f")
      userFeaturesMap.foreach{println}
      println("u->i")
      userRecommendationMap.foreach{println}
      */
      val expectedUserRecommendationMap = Map(
        "u1" -> Map("i4" -> 1.0, "i1" -> 0.83333, "i3" -> 0.666666, "i6" -> 0.5,
          "i7" -> 0.333333, "i5" -> 0.0, "i2" -> 0.0),
        "u2" -> Map("i5" -> 1.0, "i7" -> 1.0, "i4" -> 0.0, "i3" -> 0.0,
          "i1" -> 0.0, "i2" -> 0.0, "i6" -> 0.0),
        "u3" -> Map("i4" -> 0.75, "i3" -> 0.5, "i1" -> 0.5, "i7" -> 0.5,
          "i6" -> 0.5, "i5" -> 0.25, "i2" -> 0.0),
        "u4" -> Map("i4" -> 0.75, "i3" -> 0.5, "i1" -> 0.5, "i7" -> 0.5,
          "i6" -> 0.5, "i5" -> 0.25, "i2" -> 0.0))

      userRecommendationMap.keys must containTheSameElementsAs(
        expectedUserRecommendationMap.keys.toSeq)

      expectedUserRecommendationMap.map{ case(user, expected) => {
        val recommendation = userRecommendationMap(user)
        val actual = recommendation.toMap
        val valueMatched = actual must CustomMatcher.matchMapStringDouble(expected)
        // Score must be inversely sorted
        val sorted = recommendation.map(-_._2) must beSorted
        (valueMatched and sorted)
      }}.reduce(_ and _)
    }
    
    "Run with all itypes and top 2" in {
      val (userFeaturesMap, featureItypes, itemItypesMap) = (
        UserProfileRecommendation.constructUserFeaturesMapFromArg(
          appid, Some("")))
      val userRecommendationMap = UserProfileRecommendation.recommend(
        userFeaturesMap, featureItypes, itemItypesMap, 2)
    
      val expectedUserRecommendationMap = Map(
        "u1" -> Map("i4" -> 1.0, "i1" -> 0.83333),
        "u2" -> Map("i5" -> 1.0, "i7" -> 1.0),
        "u3" -> Map("i4" -> 0.75),
        "u4" -> Map("i4" -> 0.75))

      userRecommendationMap.keys must containTheSameElementsAs(
        expectedUserRecommendationMap.keys.toSeq)

      expectedUserRecommendationMap.map{ case(user, expected) => {
        val recommendation = userRecommendationMap(user)
        val actual = recommendation.toMap
        ((recommendation must have size(2)) and
          (actual must CustomMatcher.containMapStringDouble(expected)) and
          (recommendation.map(-_._2) must beSorted))
      }}.reduce(_ and _)
    }
  }

  "Realtime Run" should {
    "Run with all itypes" in {
      implicit val app = getApp(appid)
      val algoid = 1
      val modelset = true
      implicit val algo = getAlgo(algoid, modelset)
      val keyvalDb = commonConfig.getModeldataMetadataKeyvals
      val itemRecScoreDb = commonConfig.getModeldataItemRecScores

      UserProfileRecommendationRealtime.run(appid, algoid, modelset, None)

      // must find meta
      val featuresStr = keyvalDb.get(algoid, modelset, "features")
      featuresStr.isEmpty must beFalse
      featuresStr.get.split(',').toSeq must containTheSameElementsAs(
        Seq("t1", "t2", "t3", "t4"))

      // must see all users from modeldata
      rawUsers.map { uid => {
        itemRecScoreDb.getByUid(uid).isEmpty must beFalse
      }}.reduce(_ and _)
    }
    
    "Run with t1,t2" in {
      implicit val app = getApp(appid)
      val algoid = 2
      val modelset = true
      implicit val algo = getAlgo(algoid, modelset)
      val keyvalDb = commonConfig.getModeldataMetadataKeyvals
      val itemRecScoreDb = commonConfig.getModeldataItemRecScores

      UserProfileRecommendationRealtime.run(appid, algoid, modelset,
        Some("t1,t2"))

      // must find meta
      val featuresStr = keyvalDb.get(algoid, modelset, "features")
      featuresStr.isEmpty must beFalse
      // Since we specify the whitelist, therefore the featureStr will be in the
      // same order.
      featuresStr.get must be_==("t1,t2")
      
      val expectedUserFeaturesMap = Map(
        "u1" -> Seq(0.6, 0.4),
        "u2" -> Seq(0.5, 0.5),
        "u3" -> Seq(0.5, 0.5),
        "u4" -> Seq(0.5, 0.5))

      expectedUserFeaturesMap.map { case(user, features) => {
        val optUserScore = itemRecScoreDb.getByUid(user)
        optUserScore.isEmpty must beFalse
        val userScore = optUserScore.get
        userScore.scores must CustomMatcher.matchSeqDouble(features)
      }}.reduce(_ and _)
    }
  }

  "Batch Run" should {
    "Run with all itypes" in {
      implicit val app = getApp(appid)
      val algoid = 3
      val modelset = true
      val numRecommendations = 10
      implicit val algo = getAlgo(algoid, modelset)
      val keyvalDb = commonConfig.getModeldataMetadataKeyvals
      val itemRecScoreDb = commonConfig.getModeldataItemRecScores

      UserProfileRecommendationBatch.run(appid, algoid, modelset,
        numRecommendations, None)

      val expectedUserRecommendationMap = Map(
        "u1" -> Map("i4" -> 1.0, "i1" -> 0.83333, "i3" -> 0.666666, "i6" -> 0.5,
          "i7" -> 0.333333, "i5" -> 0.0, "i2" -> 0.0),
        "u2" -> Map("i5" -> 1.0, "i7" -> 1.0, "i4" -> 0.0, "i3" -> 0.0,
          "i1" -> 0.0, "i2" -> 0.0, "i6" -> 0.0),
        "u3" -> Map("i4" -> 0.75, "i3" -> 0.5, "i1" -> 0.5, "i7" -> 0.5,
          "i6" -> 0.5, "i5" -> 0.25, "i2" -> 0.0),
        "u4" -> Map("i4" -> 0.75, "i3" -> 0.5, "i1" -> 0.5, "i7" -> 0.5,
          "i6" -> 0.5, "i5" -> 0.25, "i2" -> 0.0))
      
      // must see all users from modeldata
      rawUsers.map { uid => {
        val optItemRecScore = itemRecScoreDb.getByUid(uid)
        optItemRecScore.isEmpty must beFalse

        val itemRecScore = optItemRecScore.get

        // check if values match
        val actual = itemRecScore.iids.zip(itemRecScore.scores).toMap
        val expected = expectedUserRecommendationMap(uid)
        actual must matchMapStringDouble(expected)

        // check if the itypes all passed to modelset
        val iidItypesMap = itemRecScore.iids.zip(itemRecScore.itypes).toMap
        iidItypesMap.map{ case (iid, itypes) => {
          val expectedItypes = rawItems(iid)
          itypes must containTheSameElementsAs(expectedItypes)
        }}.reduce(_ and _)
      }}.reduce(_ and _)
    }

  }
  step(cleanUp())
}
