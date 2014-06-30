package io.prediction.commons.settings

import io.prediction.commons.Spec

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class EngineInfosSpec extends Specification {
  def is = s2"""

  PredictionIO EngineInfos Specification

    EngineInfos can be implemented by:
    - MongoEngineInfos ${mongoEngineInfos}

  """

  def mongoEngineInfos = s2"""

    MongoEngineInfos should
    - behave like any EngineInfos implementation ${engineInfos(newMongoEngineInfos)}
    - (database cleanup) ${Step(Spec.mongoClient(mongoDbName).dropDatabase())}

  """

  def engineInfos(engineInfos: EngineInfos) = s2"""

    create and get an engine info ${insertAndGet(engineInfos)}
    update an engine info ${update(engineInfos)}
    delete an engine info ${delete(engineInfos)}
    backup and restore existing engine info ${backuprestore(engineInfos)}

  """

  val mongoDbName = "predictionio_mongoengineinfos_test"
  def newMongoEngineInfos = new mongodb.MongoEngineInfos(Spec.mongoClient(mongoDbName))

  def insertAndGet(engineInfos: EngineInfos) = {
    val itemrec = EngineInfo(
      id = "itemrec",
      name = "Item Recommendation Engine",
      description = Some("Recommend interesting items to each user personally."),
      params = Map[String, Param]("numRecs" -> Param(id = "numRecs", name = "", description = None, defaultvalue = 500, constraint = ParamIntegerConstraint(), ui = ParamUI(), scopes = None)),
      paramsections = Seq(),
      defaultalgoinfoid = "mahout-itembased",
      defaultofflineevalmetricinfoid = "metric-x",
      defaultofflineevalsplitterinfoid = "splitter-y")
    engineInfos.insert(itemrec)
    engineInfos.get("itemrec") must beSome(itemrec)
  }

  def update(engineInfos: EngineInfos) = {
    val itemsim = EngineInfo(
      id = "itemsim",
      name = "Items Similarity Prediction Engine",
      description = Some("Discover similar items."),
      params = Map[String, Param](),
      paramsections = Seq(),
      defaultalgoinfoid = "knnitembased",
      defaultofflineevalmetricinfoid = "metric-x",
      defaultofflineevalsplitterinfoid = "splitter-y")
    engineInfos.insert(itemsim)
    val updatedItemsim = itemsim.copy(
      defaultalgoinfoid = "mahout-itembasedcf",
      defaultofflineevalmetricinfoid = "metric-apple",
      defaultofflineevalsplitterinfoid = "splitter-orange")
    engineInfos.update(updatedItemsim)
    engineInfos.get("itemsim") must beSome(updatedItemsim)
  }

  def delete(engineInfos: EngineInfos) = {
    val foo = EngineInfo(
      id = "foo",
      name = "bar",
      description = None,
      params = Map[String, Param](),
      paramsections = Seq(),
      defaultalgoinfoid = "baz",
      defaultofflineevalmetricinfoid = "food",
      defaultofflineevalsplitterinfoid = "yummy")
    engineInfos.insert(foo)
    engineInfos.delete("foo")
    engineInfos.get("foo") must beNone
  }

  def backuprestore(engineInfos: EngineInfos) = {
    val baz = EngineInfo(
      id = "baz",
      name = "beef",
      description = Some("dead"),
      params = Map[String, Param]("abc" -> Param(id = "abc", name = "", description = None, defaultvalue = 123.4, constraint = ParamDoubleConstraint(), ui = ParamUI(), scopes = None)),
      paramsections = Seq(),
      defaultalgoinfoid = "bar",
      defaultofflineevalmetricinfoid = "yummy",
      defaultofflineevalsplitterinfoid = "food")
    engineInfos.insert(baz)
    val fn = "engineinfos.json"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(engineInfos.backup())
    } finally {
      fos.close()
    }
    engineInfos.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.UTF8).mkString.getBytes("UTF-8")) map { rengineinfos =>
      rengineinfos must contain(baz)
    } getOrElse 1 === 2
  }
}
