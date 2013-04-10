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
    "get info of algos by their engine type"                                  ! getByEngineInfoId(algoinfos) ^
                                                                              bt
  }

  def newCodeAlgoInfos = new code.CodeAlgoInfos

  def get(algoinfos: AlgoInfos) = {
    algoinfos.get("pdio-knnitembased").get.name must beEqualTo("kNN Item Based Collaborative Filtering")
  }

  def getByEngineInfoId(algoinfos: AlgoInfos) = {
    algoinfos.getByEngineInfoId("itemrec").size must beEqualTo(11)
  }
}
