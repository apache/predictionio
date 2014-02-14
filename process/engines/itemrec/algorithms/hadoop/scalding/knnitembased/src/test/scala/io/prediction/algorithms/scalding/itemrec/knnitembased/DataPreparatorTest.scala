package io.prediction.algorithms.scalding.itemrec.knnitembased

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{ Items, U2iActions }
import io.prediction.commons.filepath.DataFile

class DataPreparatorTest extends Specification with TupleConversions {

  val Rate = "rate"
  val Like = "like"
  val Dislike = "dislike"
  val View = "view"
  //val ViewDetails = "viewDetails"
  val Conversion = "conversion"

  val appid = 2

  def test(itypes: List[String], params: Map[String, String],
    items: List[(String, String, String, String, String, String)], // id, itypes, appid, starttime, ct, endtime
    u2iActions: List[(String, String, String, String, String)],
    ratings: List[(String, String, Int)],
    selectedItems: List[(String, String, String, String)] // id, itypes, starttime, endtime
    ) = {

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None //Option("testhost")
    val dbPort = None //Option(27017)
    val hdfsRoot = "testroot/"

    JobTest("io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      //.arg("dbHost", dbHost.get)
      //.arg("dbPort", dbPort.get.toString)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", "4")
      .arg("algoid", "5")
      .arg("itypes", itypes)
      .arg("viewParam", params("viewParam"))
      .arg("likeParam", params("likeParam"))
      .arg("dislikeParam", params("dislikeParam"))
      .arg("conversionParam", params("conversionParam"))
      .arg("conflictParam", params("conflictParam"))
      //.arg("debug", List("test")) // NOTE: test mode
      .source(Items(appId = appid, itypes = Some(itypes), dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort).getSource, items)
      .source(U2iActions(appId = appid, dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort).getSource, u2iActions)
      .sink[(String, String, Int)](Tsv(DataFile(hdfsRoot, 2, 4, 5, None, "ratings.tsv"))) { outputBuffer =>
        "correctly process and write data to ratings.tsv" in {
          outputBuffer.toList must containTheSameElementsAs(ratings)
        }
      }
      .sink[(String, String, String, String)](Tsv(DataFile(hdfsRoot, 2, 4, 5, None, "selectedItems.tsv"))) { outputBuffer =>
        "correctly write selectedItems.tsv" in {
          outputBuffer.toList must containTheSameElementsAs(selectedItems)
        }
      }
      .run
      .finish

  }

  /** no itypes specified */
  def testWithoutItypes(params: Map[String, String],
    items: List[(String, String, String, String, String, String)], // id, itypes, appid, starttime, ct, endtime
    u2iActions: List[(String, String, String, String, String)],
    ratings: List[(String, String, Int)],
    selectedItems: List[(String, String, String, String)] // id, itypes, starttime, endtime
    ) = {

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None //Option("testhost")
    val dbPort = None //Option(27017)
    val hdfsRoot = "testroot/"

    JobTest("io.prediction.algorithms.scalding.itemrec.knnitembased.DataPreparator")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      //.arg("dbHost", dbHost.get)
      //.arg("dbPort", dbPort.get.toString)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", "4")
      .arg("algoid", "5")
      //.arg("itypes", itypes) // NOTE: no itypes args!
      .arg("viewParam", params("viewParam"))
      .arg("likeParam", params("likeParam"))
      .arg("dislikeParam", params("dislikeParam"))
      .arg("conversionParam", params("conversionParam"))
      .arg("conflictParam", params("conflictParam"))
      //.arg("debug", List("test")) // NOTE: test mode
      .source(Items(appId = appid, itypes = None, dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort).getSource, items)
      .source(U2iActions(appId = appid, dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort).getSource, u2iActions)
      .sink[(String, String, Int)](Tsv(DataFile(hdfsRoot, 2, 4, 5, None, "ratings.tsv"))) { outputBuffer =>
        "correctly process and write data to ratings.tsv" in {
          outputBuffer.toList must containTheSameElementsAs(ratings)
        }
      }
      .sink[(String, String, String, String)](Tsv(DataFile(hdfsRoot, 2, 4, 5, None, "selectedItems.tsv"))) { outputBuffer =>
        "correctly write selectedItems.tsv" in {
          outputBuffer.toList must containTheSameElementsAs(selectedItems)
        }
      }
      .run
      .finish

  }

  val noEndtime = "PIO_NONE"
  /**
   * Test 1. basic. Rate actions only without conflicts
   */
  val test1AllItypes = List("t1", "t2", "t3", "t4")
  val test1ItemsMap = Map(
    // id, itypes, appid, starttime, ct, endtime
    "i0" -> ("i0", "t1,t2,t3", appid.toString, "12345", "12346", noEndtime),
    "i1" -> ("i1", "t2,t3", appid.toString, "12347", "12348", noEndtime),
    "i2" -> ("i2", "t4", appid.toString, "12349", "12350", noEndtime),
    "i3" -> ("i3", "t3,t4", appid.toString, "12351", "12352", noEndtime))

  val test1Items = List(
    test1ItemsMap("i0"),
    test1ItemsMap("i1"),
    test1ItemsMap("i2"),
    test1ItemsMap("i3"))

  def genSelectedItems(items: List[(String, String, String, String, String, String)]) = {
    items map { x =>
      val (id, itypes, appid, starttime, ct, endtime) = x
      (id, itypes, starttime, endtime)
    }
  }

  val test1U2i = List(
    (Rate, "u0", "i0", "123450", "3"),
    (Rate, "u0", "i1", "123457", "1"),
    (Rate, "u0", "i2", "123458", "4"),
    (Rate, "u0", "i3", "123459", "2"),
    (Rate, "u1", "i0", "123457", "5"),
    (Rate, "u1", "i1", "123458", "2"))

  val test1Ratings = List(
    ("u0", "i0", 3),
    ("u0", "i1", 1),
    ("u0", "i2", 4),
    ("u0", "i3", 2),
    ("u1", "i0", 5),
    ("u1", "i1", 2))

  val test1Params: Map[String, String] = Map("viewParam" -> "3", "likeParam" -> "4", "dislikeParam" -> "1", "conversionParam" -> "5",
    "conflictParam" -> "latest")

  "itemrec.knnitembased DataPreparator with only rate actions, all itypes, no conflict" should {
    test(test1AllItypes, test1Params, test1Items, test1U2i, test1Ratings, genSelectedItems(test1Items))
  }

  "itemrec.knnitembased DataPreparator with only rate actions, no itypes specified, no conflict" should {
    testWithoutItypes(test1Params, test1Items, test1U2i, test1Ratings, genSelectedItems(test1Items))
  }

  /**
   * Test 2. rate actions only with conflicts
   */
  val test2AllItypes = List("t1", "t2", "t3", "t4")
  val test2ItemsMap = Map(
    // id, itypes, appid, starttime, ct, endtime
    "i0" -> ("i0", "t1,t2,t3", appid.toString, "12345", "12346", noEndtime),
    "i1" -> ("i1", "t2,t3", appid.toString, "12347", "12348", noEndtime),
    "i2" -> ("i2", "t4", appid.toString, "12349", "12350", noEndtime),
    "i3" -> ("i3", "t3,t4", appid.toString, "12351", "12352", noEndtime))

  val test2Items = List(
    test2ItemsMap("i0"),
    test2ItemsMap("i1"),
    test2ItemsMap("i2"),
    test2ItemsMap("i3"))

  val test2U2i = List(
    (Rate, "u0", "i0", "123448", "3"),
    (Rate, "u0", "i0", "123449", "4"), // highest
    (Rate, "u0", "i0", "123451", "2"), // latest 
    (Rate, "u0", "i0", "123450", "1"), // lowest

    (Rate, "u0", "i1", "123456", "1"), // lowest
    (Rate, "u0", "i1", "123457", "2"),
    (Rate, "u0", "i1", "123458", "3"), // latest, highest

    (Rate, "u0", "i2", "123461", "2"), // latest, lowest
    (Rate, "u0", "i2", "123459", "3"),
    (Rate, "u0", "i2", "123460", "5"), // highest

    (Rate, "u0", "i3", "123459", "2"),
    (Rate, "u1", "i0", "123457", "5"),

    (Rate, "u1", "i1", "123458", "3"), // lowest
    (Rate, "u1", "i1", "123459", "4"), // highest
    (Rate, "u1", "i1", "123460", "3")) // latest, lowest

  val test2RatingsLatest = List(
    ("u0", "i0", 2),
    ("u0", "i1", 3),
    ("u0", "i2", 2),
    ("u0", "i3", 2),
    ("u1", "i0", 5),
    ("u1", "i1", 3))

  val test2RatingsHighest = List(
    ("u0", "i0", 4),
    ("u0", "i1", 3),
    ("u0", "i2", 5),
    ("u0", "i3", 2),
    ("u1", "i0", 5),
    ("u1", "i1", 4))

  val test2RatingsLowest = List(
    ("u0", "i0", 1),
    ("u0", "i1", 1),
    ("u0", "i2", 2),
    ("u0", "i3", 2),
    ("u1", "i0", 5),
    ("u1", "i1", 3))

  val test2Itypes_t1t4 = List("t1", "t4")
  val test2Items_t1t4 = List(
    test2ItemsMap("i0"),
    test2ItemsMap("i2"),
    test2ItemsMap("i3"))
  val test2RatingsHighest_t1t4 = List(
    ("u0", "i0", 4),
    ("u0", "i2", 5),
    ("u0", "i3", 2),
    ("u1", "i0", 5))

  val test2Params: Map[String, String] = Map("viewParam" -> "3", "likeParam" -> "4", "dislikeParam" -> "1", "conversionParam" -> "5",
    "conflictParam" -> "latest")
  val test2ParamsHighest = test2Params + ("conflictParam" -> "highest")
  val test2ParamsLowest = test2Params + ("conflictParam" -> "lowest")

  "itemrec.knnitembased DataPreparator with only rate actions, all itypes, conflict=latest" should {
    test(test2AllItypes, test2Params, test2Items, test2U2i, test2RatingsLatest, genSelectedItems(test2Items))
  }

  "itemrec.knnitembased DataPreparator with only rate actions, all itypes, conflict=highest" should {
    test(test2AllItypes, test2ParamsHighest, test2Items, test2U2i, test2RatingsHighest, genSelectedItems(test2Items))
  }

  "itemrec.knnitembased DataPreparator with only rate actions, all itypes, conflict=lowest" should {
    test(test2AllItypes, test2ParamsLowest, test2Items, test2U2i, test2RatingsLowest, genSelectedItems(test2Items))
  }

  "itemrec.knnitembased DataPreparator with only rate actions, some itypes, conflict=highest" should {
    test(test2Itypes_t1t4, test2ParamsHighest, test2Items, test2U2i, test2RatingsHighest_t1t4, genSelectedItems(test2Items_t1t4))
  }

  /**
   * Test 3. Different Actions without conflicts
   */
  val test3AllItypes = List("t1", "t2", "t3", "t4")
  val test3ItemsMap = Map(
    // id, itypes, appid, starttime, ct, endtime
    "i0" -> ("i0", "t1,t2,t3", appid.toString, "12345", "12346", "56789"),
    "i1" -> ("i1", "t2,t3", appid.toString, "12347", "12348", noEndtime),
    "i2" -> ("i2", "t4", appid.toString, "12349", "12350", "56790"),
    "i3" -> ("i3", "t3,t4", appid.toString, "12351", "12352", noEndtime))

  val test3Items = List(
    test3ItemsMap("i0"),
    test3ItemsMap("i1"),
    test3ItemsMap("i2"),
    test3ItemsMap("i3"))

  val test3U2i = List(
    (Rate, "u0", "i0", "123450", "4"),
    (Like, "u0", "i1", "123457", "PIO_NONE"),
    (Dislike, "u0", "i2", "123458", "PIO_NONE"),
    (View, "u0", "i3", "123459", "PIO_NONE"), // NOTE: assume v field won't be missing
    (Rate, "u1", "i0", "123457", "2"),
    (Conversion, "u1", "i1", "123458", "PIO_NONE"))

  val test3Ratings = List(
    ("u0", "i0", 4),
    ("u0", "i1", 4),
    ("u0", "i2", 2),
    ("u0", "i3", 1),
    ("u1", "i0", 2),
    ("u1", "i1", 5))

  val test3Params: Map[String, String] = Map("viewParam" -> "1", "likeParam" -> "4", "dislikeParam" -> "2", "conversionParam" -> "5",
    "conflictParam" -> "latest")

  "itemrec.knnitembased DataPreparator with only all actions, all itypes, no conflict" should {
    test(test3AllItypes, test3Params, test3Items, test3U2i, test3Ratings, genSelectedItems(test3Items))
  }

  /**
   * test 4. Different Actions with conflicts
   */
  val test4Params: Map[String, String] = Map("viewParam" -> "2", "likeParam" -> "5", "dislikeParam" -> "1", "conversionParam" -> "4",
    "conflictParam" -> "latest")

  val test4AllItypes = List("t1", "t2", "t3", "t4")
  val test4ItemsMap = Map(
    // id, itypes, appid, starttime, ct, endtime
    "i0" -> ("i0", "t1,t2,t3", appid.toString, "12345", "12346", "56789"),
    "i1" -> ("i1", "t2,t3", appid.toString, "12347", "12348", noEndtime),
    "i2" -> ("i2", "t4", appid.toString, "12349", "12350", "56790"),
    "i3" -> ("i3", "t3,t4", appid.toString, "12351", "12352", noEndtime))

  val test4Items = List(
    test4ItemsMap("i0"),
    test4ItemsMap("i1"),
    test4ItemsMap("i2"),
    test4ItemsMap("i3"))

  val test4U2i = List(
    (Rate, "u0", "i0", "123448", "3"),
    (View, "u0", "i0", "123449", "PIO_NONE"), // lowest (2)
    (Like, "u0", "i0", "123451", "PIO_NONE"), // latest, highest (5)
    (Conversion, "u0", "i0", "123450", "PIO_NONE"),

    (Rate, "u0", "i1", "123456", "1"), // lowest
    (Rate, "u0", "i1", "123457", "4"), // highest
    (View, "u0", "i1", "123458", "PIO_NONE"), // latest (2)

    (Conversion, "u0", "i2", "123461", "PIO_NONE"), // latest, highest  (4)
    (Rate, "u0", "i2", "123459", "3"),
    (View, "u0", "i2", "123460", "PIO_NONE"), // lowest

    (Rate, "u0", "i3", "123459", "2"),
    (View, "u1", "i0", "123457", "PIO_NONE"), // (2)

    (Rate, "u1", "i1", "123458", "5"), // highest
    (Conversion, "u1", "i1", "123459", "PIO_NONE"), // (4)
    (Dislike, "u1", "i1", "123460", "PIO_NONE")) // latest, lowest (1)

  val test4RatingsLatest = List(
    ("u0", "i0", 5),
    ("u0", "i1", 2),
    ("u0", "i2", 4),
    ("u0", "i3", 2),
    ("u1", "i0", 2),
    ("u1", "i1", 1))

  "itemrec.knnitembased DataPreparator with all actions, all itypes, and conflicts=latest" should {
    test(test4AllItypes, test4Params, test4Items, test4U2i, test4RatingsLatest, genSelectedItems(test4Items))
  }

  val test4ParamsIgnoreView = test4Params + ("viewParam" -> "ignore")

  val test4RatingsIgnoreViewLatest = List(
    ("u0", "i0", 5),
    ("u0", "i1", 4),
    ("u0", "i2", 4),
    ("u0", "i3", 2),
    ("u1", "i1", 1))

  "itemrec.knnitembased DataPreparator with all actions, all itypes, ignore View actions and conflicts=latest" should {
    test(test4AllItypes, test4ParamsIgnoreView, test4Items, test4U2i, test4RatingsIgnoreViewLatest, genSelectedItems(test4Items))
  }

  // note: currently rate action can't be ignored
  val test4ParamsIgnoreAllExceptView = test4Params + ("viewParam" -> "1", "likeParam" -> "ignore", "dislikeParam" -> "ignore", "conversionParam" -> "ignore")

  val test4RatingsIgnoreAllExceptViewLatest = List(
    ("u0", "i0", 1),
    ("u0", "i1", 1),
    ("u0", "i2", 1),
    ("u0", "i3", 2),
    ("u1", "i0", 1),
    ("u1", "i1", 5))

  "itemrec.knnitembased DataPreparator with all actions, all itypes, ignore all actions except View (and Rate) and conflicts=latest" should {
    test(test4AllItypes, test4ParamsIgnoreAllExceptView, test4Items, test4U2i, test4RatingsIgnoreAllExceptViewLatest, genSelectedItems(test4Items))
  }

  // note: meaning rate action only
  val test4ParamsIgnoreAll = test4Params + ("viewParam" -> "ignore", "likeParam" -> "ignore", "dislikeParam" -> "ignore", "conversionParam" -> "ignore")

  val test4RatingsIgnoreAllLatest = List(
    ("u0", "i0", 3),
    ("u0", "i1", 4),
    ("u0", "i2", 3),
    ("u0", "i3", 2),
    ("u1", "i1", 5))

  "itemrec.knnitembased DataPreparator with all actions, all itypes, ignore all actions (except Rate) and conflicts=latest" should {
    test(test4AllItypes, test4ParamsIgnoreAll, test4Items, test4U2i, test4RatingsIgnoreAllLatest, genSelectedItems(test4Items))
  }

  val test4ParamsLowest: Map[String, String] = test4Params + ("conflictParam" -> "lowest")

  val test4Itypes_t3 = List("t3")
  val test4Items_t3 = List(
    test4ItemsMap("i0"),
    test4ItemsMap("i1"),
    test4ItemsMap("i3"))

  val test4RatingsLowest_t3 = List(
    ("u0", "i0", 2),
    ("u0", "i1", 1),
    ("u0", "i3", 2),
    ("u1", "i0", 2),
    ("u1", "i1", 1))

  "itemrec.knnitembased DataPreparator with all actions, some itypes, and conflicts=lowest" should {
    test(test4Itypes_t3, test4ParamsLowest, test4Items, test4U2i, test4RatingsLowest_t3, genSelectedItems(test4Items_t3))
  }

}
