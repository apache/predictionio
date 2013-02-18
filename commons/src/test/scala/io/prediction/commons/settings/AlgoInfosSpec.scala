package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step

class AlgoInfosSpec extends Specification { def is =
  "PredictionIO AlgoInfos Specification"                                      ^
                                                                              p ^
  "Algos can be implemented by:"                                              ^ endp ^
    "1. CodeAlgoInfos"                                                        ^ codeAlgoInfos^end

  def codeAlgoInfos =                                                         p ^
    "CodeAlgoInfos should"                                                    ^
      "behave like any AlgoInfos implementation"                              ^ algoinfos(newCodeAlgoInfos)

  def algoinfos(algoinfos: AlgoInfos) = {                                     t ^
    "get info of an algo by its ID"                                           ! get(algoinfos) ^
    "get info of algos by their engine type"                                  ! getByEngineType(algoinfos) ^
                                                                              bt
  }

  def newCodeAlgoInfos = new code.CodeAlgoInfos

  def get(algoinfos: AlgoInfos) = {
    algoinfos.get("io.prediction.algorithms.scalding.itemrec.knnitembased").get.name must beEqualTo("kNN Item Based Collaborative Filtering")
  }

  def getByEngineType(algoinfos: AlgoInfos) = {
    algoinfos.getByEngineType("itemrec").size must beEqualTo(1)
  }
}
