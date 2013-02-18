package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step

class AlgoInfosSpec extends Specification { def is =
  "PredictionIO AlgoInfos Specification"                                      ^
                                                                              p ^
  "Algos can be implemented by:"                                              ^ endp ^
    "1. ScalaAlgoInfos"                                                       ^ scalaAlgoInfos^end

  def scalaAlgoInfos =                                                        p ^
    "ScalaAlgoInfos should"                                                   ^
      "behave like any AlgoInfos implementation"                              ^ algoinfos(newScalaAlgoInfos)

  def algoinfos(algoinfos: AlgoInfos) = {                                     t ^
    "get info of an algo by its ID"                                           ! get(algoinfos) ^
    "get info of algos by their engine type"                                  ! getByEngineType(algoinfos) ^
                                                                              bt
  }

  def newScalaAlgoInfos = new scala.ScalaAlgoInfos

  def get(algoinfos: AlgoInfos) = {
    algoinfos.get("io.prediction.algorithms.scalding.itemrec.knnitembased").get.name must beEqualTo("kNN Item Based Collaborative Filtering")
  }

  def getByEngineType(algoinfos: AlgoInfos) = {
    algoinfos.getByEngineType("itemrec").size must beEqualTo(1)
  }
}
