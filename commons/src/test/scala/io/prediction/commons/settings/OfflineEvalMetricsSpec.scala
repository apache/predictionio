package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class OfflineEvalMetricsSpec extends Specification { def is =
  "PredictionIO OfflineEvalMetrics Specification"               ^
                                                                p^
  "OfflineEvalMetrics can be implemented by:"                   ^ endp^
    "1. MongoOfflineEvalMetrics"                                ^ mongoOfflineEvalMetrics^end

  def mongoOfflineEvalMetrics =                                 p^
    "MongoOfflineEvalMetrics should"                            ^
      "behave like any OfflineEvalMetrics implementation"       ^ offlineEvalMetricsTest(newMongoOfflineEvalMetrics)^
                                                                Step(MongoConnection()(mongoDbName).dropDatabase())

  def offlineEvalMetricsTest(offlineEvalMetrics: OfflineEvalMetrics) = {    t^
    "create an OfflineEvalMetric"                                           ! insert(offlineEvalMetrics)^
    "get two OfflineEvalMetrics"                                            ! getByEvalid(offlineEvalMetrics)^
    "update an OfflineEvalMetric"                                           ! update(offlineEvalMetrics)^
    "delete an OfflineEvalMetric"                                           ! delete(offlineEvalMetrics)^
                                                                            bt
  }

  val mongoDbName = "predictionio_mongoofflineevalmetrics_test"
  def newMongoOfflineEvalMetrics = new mongodb.MongoOfflineEvalMetrics(MongoConnection()(mongoDbName))

  /**
   * insert and get by id
   */
  def insert(offlineEvalMetrics: OfflineEvalMetrics) = {
    val obj = OfflineEvalMetric(
      id = -1,
      name = "metric-insert1",
      metrictype = "metric-insert-type1",
      evalid = 42,
      params = Map(("abc" -> 3), ("bar" -> "foo1 foo2"))
    )

    val insertid = offlineEvalMetrics.insert(obj)
    offlineEvalMetrics.get(insertid) must beSome(obj.copy(id = insertid))
  }

  /**
   * insert a few and get by engineid
   */
  def getByEvalid(offlineEvalMetrics: OfflineEvalMetrics) = {
    val obj1 = OfflineEvalMetric(
      id = -1,
      name = "metric-getByEvalid1",
      metrictype = "metric-getByEvalid-type1",
      evalid = 15,
      params = Map(("abc1" -> 6), ("bar1" -> "foo1 foo2"), ("bar1b" -> "foo1b"))
    )
    val obj2 = OfflineEvalMetric(
      id = -1,
      name = "metric-getByEvalid2",
      metrictype = "metric-getByEvalid-type2",
      evalid = 15,
      params = Map(("abc2" -> 0), ("bar2" -> "foox"))
    )

    val id1 = offlineEvalMetrics.insert(obj1)
    val id2 = offlineEvalMetrics.insert(obj2)

    val it = offlineEvalMetrics.getByEvalid(15)

    val it1 = it.next()
    val it2 = it.next()
    val left = it.hasNext // make sure it has 2 only

    it1 must be equalTo(obj1.copy(id = id1)) and
      (it2 must be equalTo(obj2.copy(id = id2))) and
      (left must be_==(false))

  }

  /**
   * insert one and then update with new data and get back
   */
  def update(offlineEvalMetrics: OfflineEvalMetrics) = {
    val obj1 = OfflineEvalMetric(
      id = -1,
      name = "metric-update1",
      metrictype = "metric-update-type1",
      evalid = 16,
      params = Map(("def" -> "a1 a2 a3"), ("def2" -> 1), ("def3" -> "food"))
    )

    val updateid = offlineEvalMetrics.insert(obj1)
    val data1 = offlineEvalMetrics.get(updateid)

    val obj2 = obj1.copy(
      id = updateid,
      name = "metric-update2",
      metrictype = "metric-update-type2",
      evalid = 99,
      params = Map()
    )

    offlineEvalMetrics.update(obj2)

    val data2 = offlineEvalMetrics.get(updateid)

    data1 must beSome(obj1.copy(id = updateid)) and
      (data2 must beSome(obj2))

  }

  /**
   * insert one and delete and get back
   */
  def delete(offlineEvalMetrics: OfflineEvalMetrics) = {
    val obj1 = OfflineEvalMetric(
      id = -1,
      name = "metric-delete1",
      metrictype = "metric-delete-type1",
      evalid = 3,
      params = Map(("x" -> 1))
    )

    val id1 = offlineEvalMetrics.insert(obj1)
    val data1 = offlineEvalMetrics.get(id1)

    offlineEvalMetrics.delete(id1)
    val data2 = offlineEvalMetrics.get(id1)

    data1 must beSome(obj1.copy(id = id1)) and
      (data2 must beNone)

  }

}