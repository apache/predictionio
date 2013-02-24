package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class OfflineEvalResultsSpec extends Specification { def is =
  "PredictionIO OfflineEvalResults Specification"               ^
                                                                p^
  "OfflineEvalResults can be implemented by:"                   ^ endp^
    "1. MongoOfflineEvalResults"                                ^ mongoOfflineEvalResults^end

  def mongoOfflineEvalResults =                                 p^
    "MongoOfflineEvalResults should"                            ^
      "behave like any OfflineEvalResults implementation"       ^ offlineEvalResultsTest(newMongoOfflineEvalResults)^
                                                                Step(MongoConnection()(mongoDbName).dropDatabase())

  def offlineEvalResultsTest(offlineEvalResults: OfflineEvalResults) = {    t^
    "get two OfflineEvalResults by evalid"                                  ! getByEvalid(offlineEvalResults)^
    "delete two OfflineEvalResults by evalid"                               ! deleteByEvalid(offlineEvalResults)^
                                                                            bt
  }

  val mongoDbName = "predictionio_mongoofflineevalresults_test"
  def newMongoOfflineEvalResults = new mongodb.MongoOfflineEvalResults(MongoConnection()(mongoDbName))

  /**
   * save a few and get by evalid
   */
  def getByEvalid(offlineEvalResults: OfflineEvalResults) = {
    val obj1 = OfflineEvalResult(
      id = "-1",
      evalid = 16,
      metricid = 2,
      algoid = 3,
      score = 0.09876
    )
    val obj2 = OfflineEvalResult(
      id = "-1",
      evalid = 16,
      metricid = 3,
      algoid = 4,
      score = 0.123
    )
    val obj3 = OfflineEvalResult(
      id = "-1",
      evalid = 2,
      metricid = 3,
      algoid = 4,
      score = 0.567
    )

    val id1 = offlineEvalResults.save(obj1)
    val id2 = offlineEvalResults.save(obj2)
    val id3 = offlineEvalResults.save(obj3)

    val it = offlineEvalResults.getByEvalid(16)

    val itData1 = it.next()
    val itData2 = it.next()

    val itData3 = offlineEvalResults.getByEvalidAndMetricidAndAlgoid(2, 3, 4)

    itData1 must be equalTo(obj1.copy(id = id1)) and
      (itData2 must be equalTo(obj2.copy(id = id2))) and
      (it.hasNext must be_==(false)) and // make sure it has 2 only
      (itData3 must beSome(obj3.copy(id = id3)))
  }

  /**
   * save a few and delete by evalid and get back
   */
  def deleteByEvalid(offlineEvalResults: OfflineEvalResults) = {
    val obj1 = OfflineEvalResult(
      id = "-1",
      evalid = 25,
      metricid = 6,
      algoid = 8,
      score = 0.7601
    )
    val obj2 = OfflineEvalResult(
      id = "-1",
      evalid = 7,
      metricid = 1,
      algoid = 9,
      score = 0.001
    )
    val obj3 = OfflineEvalResult(
      id = "-1",
      evalid = 25,
      metricid = 33,
      algoid = 41,
      score = 0.999
    )

    val id1 = offlineEvalResults.save(obj1)
    val id2 = offlineEvalResults.save(obj2)
    val id3 = offlineEvalResults.save(obj3)

    val it1 = offlineEvalResults.getByEvalid(25)

    val it1Data1 = it1.next()
    val it1Data2 = it1.next()

    offlineEvalResults.deleteByEvalid(25)

    val it2 = offlineEvalResults.getByEvalid(25)
    val it3 = offlineEvalResults.getByEvalid(7) // others shouldn't be deleted
    val it3Data1 = it3.next()

    it1Data1 must be equalTo(obj1.copy(id = id1)) and
      (it1Data2 must be equalTo(obj3.copy(id = id3))) and
      (it1.hasNext must be_==(false)) and //make sure it has 2 only
      (it2.hasNext must be_==(false))
      (it3Data1 must be equalTo(obj2.copy(id = id2))) and
      (it3.hasNext must be_==(false))

  }

}