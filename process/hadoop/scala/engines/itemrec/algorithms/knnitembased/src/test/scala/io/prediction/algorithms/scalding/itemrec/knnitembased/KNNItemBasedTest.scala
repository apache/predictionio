package io.prediction.algorithms.scalding.itemrec.knnitembased

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{DataFile, AlgoFile}

class KNNItemBasedTest extends Specification with TupleConversions {
   
  // helper function
  // only compare double up to 9 decimal places
  def roundingData(orgList: List[(String, String, Double)]) = {
    orgList map { x =>
      val (t1, t2, t3) = x
      
      // NOTE: use HALF_UP mode to avoid error caused by rounding when compare data
      // (eg. 3.5 vs 3.499999999999).
      // (eg. 0.6666666666 vs 0.666666667)
      
      (t1, t2, BigDecimal(t3).setScale(9, BigDecimal.RoundingMode.HALF_UP).toDouble)
    }
  }
  
  // test1
  val test1args = Map[String, String]("measureParam" -> "correl",
      "priorCountParam" -> "10",
      "priorCorrelParam" -> "0",
      "minNumRatersParam" -> "1",
      "maxNumRatersParam" -> "100000",
      "minIntersectionParam" -> "1",
      "minNumRatedSimParam" -> "1"
  )
  
  val test1Input = List(
      ("u0","i0",1),
      ("u0","i1",2),
      ("u0","i2",3),
      ("u1","i1",4),
      ("u1","i2",4),
      ("u1","i3",2),
      ("u2","i0",3),
      ("u2","i1",2),
      ("u2","i3",1),
      ("u3","i0",2),
      ("u3","i2",1),
      ("u3","i3",5))
      
  val test1ItemSimScore = List(
      ("i0","i1",0.0),
      ("i1","i0",0.0),
      ("i0","i2",-0.16666666666666666),
      ("i2","i0",-0.16666666666666666),
      ("i0","i3",-0.16666666666666666),
      ("i3","i0",-0.16666666666666666),
      ("i1","i2",0.16666666666666666),
      ("i2","i1",0.16666666666666666),
      ("i1","i3",0.16666666666666666),
      ("i3","i1",0.16666666666666666),
      ("i2","i3",-0.16666666666666666),
      ("i3","i2",-0.16666666666666666))
  
  val test1Output = List[(String, String, Double)](
      ("u0","i0",1),
      ("u0","i1",2),
      ("u0","i2",3),
      ("u0","i3",-0.666666666666667),
      ("u1","i0",-3.0),
      ("u1","i1",4),
      ("u1","i2",4),
      ("u1","i3",2),
      ("u2","i0",3),
      ("u2","i1",2),
      ("u2","i2",-0.666666666666667),
      ("u2","i3",1),
      ("u3","i0",2),
      ("u3","i1",3.0),
      ("u3","i2",1),
      ("u3","i3",5))
  
  val hdfsRoot = "testroot/"
  
  "KNNItemBasedTest minNumRatedSimParam=1 and mergeRatingParam defined" should {
    JobTest("io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased").
      arg("appid", "1").
      arg("engineid", "2").
      arg("algoid", "3").
      arg("hdfsRoot", hdfsRoot).
      arg("measureParam", test1args("measureParam")).
      arg("priorCountParam", test1args("priorCountParam")).
      arg("priorCorrelParam", test1args("priorCorrelParam")).
      arg("minNumRatersParam", test1args("minNumRatersParam")).
      arg("maxNumRatersParam", test1args("maxNumRatersParam")).
      arg("minIntersectionParam", test1args("minIntersectionParam")).
      arg("minNumRatedSimParam", test1args("minNumRatedSimParam")).
      arg("mergeRatingParam", "").
      source(Tsv(DataFile(hdfsRoot, 1, 2, 3, None, "ratings.tsv")), test1Input).
      sink[(String, String, Double)](Tsv(AlgoFile(hdfsRoot, 1,2,3,None,"itemRecScores.tsv"))) { outputBuffer =>
         "correctly calculate itemRecScores" in {
           roundingData(outputBuffer.toList) must containTheSameElementsAs(roundingData(test1Output))
         }
       }
       .run
       .finish
       
  }
 
  // test2
  val test2args = Map[String, String]("measureParam" -> "correl",
      "priorCountParam" -> "10",
      "priorCorrelParam" -> "0",
      "minNumRatersParam" -> "1",
      "maxNumRatersParam" -> "100000",
      "minIntersectionParam" -> "1",
      "minNumRatedSimParam" -> "3"
  )
  
  val test2Input = List(
      ("u0","i0",1),
      ("u0","i1",2),
      ("u0","i2",3),
      ("u1","i1",4),
      ("u1","i2",4),
      ("u1","i3",2),
      ("u2","i0",3),
      ("u2","i1",2),
      ("u2","i3",1),
      ("u3","i0",2),
      ("u3","i2",1),
      ("u3","i3",5))
      
  val test2ItemSimScore = List(
      ("i0","i1",0.0),
      ("i1","i0",0.0),
      ("i0","i2",-0.16666666666666666),
      ("i2","i0",-0.16666666666666666),
      ("i0","i3",-0.16666666666666666),
      ("i3","i0",-0.16666666666666666),
      ("i1","i2",0.16666666666666666),
      ("i2","i1",0.16666666666666666),
      ("i1","i3",0.16666666666666666),
      ("i3","i1",0.16666666666666666),
      ("i2","i3",-0.16666666666666666),
      ("i3","i2",-0.16666666666666666))
  
  val test2Output = List[(String, String, Double)](
      ("u0","i0",1),
      ("u0","i1",2),
      ("u0","i2",3),
      ("u0","i3",-0.666666666666667),
      // u1, i0, won't be predicted because num of similar items is < minNumRatedSimParam
      ("u1","i1",4),
      ("u1","i2",4),
      ("u1","i3",2),
      ("u2","i0",3),
      ("u2","i1",2),
      ("u2","i2",-0.666666666666667),
      ("u2","i3",1),
      ("u3","i0",2),
      // u3, i1, won't be predicted because num of similar items is < minNumRatedSimParam
      ("u3","i2",1),
      ("u3","i3",5))
  

  "KNNItemBasedTest minNumRatedSimParam=3 and mergeRatingParam defined" should {
    JobTest("io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased").
      arg("appid", "1").
      arg("engineid", "2").
      arg("algoid", "3").
      arg("hdfsRoot", hdfsRoot).
      arg("measureParam", test2args("measureParam")).
      arg("priorCountParam", test2args("priorCountParam")).
      arg("priorCorrelParam", test2args("priorCorrelParam")).
      arg("minNumRatersParam", test2args("minNumRatersParam")).
      arg("maxNumRatersParam", test2args("maxNumRatersParam")).
      arg("minIntersectionParam", test2args("minIntersectionParam")).
      arg("minNumRatedSimParam", test2args("minNumRatedSimParam")).
      arg("mergeRatingParam", "").
      source(Tsv(DataFile(hdfsRoot, 1, 2, 3, None, "ratings.tsv")), test2Input).
      sink[(String, String, Double)](Tsv(AlgoFile(hdfsRoot, 1,2,3,None,"itemRecScores.tsv"))) { outputBuffer =>
         "correctly calculate itemRecScores" in {
           roundingData(outputBuffer.toList) must containTheSameElementsAs(roundingData(test2Output))
         }
       }
       .run
       .finish
       
  }
  
  // test3
  val test3args = Map[String, String]("measureParam" -> "correl",
      "priorCountParam" -> "10",
      "priorCorrelParam" -> "0",
      "minNumRatersParam" -> "1",
      "maxNumRatersParam" -> "100000",
      "minIntersectionParam" -> "1",
      "minNumRatedSimParam" -> "1"
  )
  
  val test3Input = List(
      ("u0","i0",1),
      ("u0","i1",2),
      ("u0","i2",3),
      ("u1","i1",4),
      ("u1","i2",4),
      ("u1","i3",2),
      ("u2","i0",3),
      ("u2","i1",2),
      ("u2","i3",1),
      ("u3","i0",2),
      ("u3","i2",1),
      ("u3","i3",5))
      
  val test3ItemSimScore = List(
      ("i0","i1",0.0),
      ("i1","i0",0.0),
      ("i0","i2",-0.16666666666666666),
      ("i2","i0",-0.16666666666666666),
      ("i0","i3",-0.16666666666666666),
      ("i3","i0",-0.16666666666666666),
      ("i1","i2",0.16666666666666666),
      ("i2","i1",0.16666666666666666),
      ("i1","i3",0.16666666666666666),
      ("i3","i1",0.16666666666666666),
      ("i2","i3",-0.16666666666666666),
      ("i3","i2",-0.16666666666666666))
  
  val test3Output = List[(String, String, Double)](
      ("u0","i0",-3.0),
      ("u0","i1",3.0),
      ("u0","i2",0.5),
      ("u0","i3",-0.666666666666667),
      ("u1","i0",-3.0),
      ("u1","i1",3.0),
      ("u1","i2",1.0),
      ("u1","i3",0.0),
      ("u2","i0",-1.0),
      ("u2","i1",1.0),
      ("u2","i2",-0.666666666666667),
      ("u2","i3",-0.5),
      ("u3","i0",-3.0),
      ("u3","i1",3.0),
      ("u3","i2",-3.5),
      ("u3","i3",-1.5))
  

  "KNNItemBasedTest without mergeRatingParam defined" should {
    JobTest("io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased").
      arg("appid", "1").
      arg("engineid", "2").
      arg("algoid", "3").
      arg("hdfsRoot", hdfsRoot).
      arg("measureParam", test3args("measureParam")).
      arg("priorCountParam", test3args("priorCountParam")).
      arg("priorCorrelParam", test3args("priorCorrelParam")).
      arg("minNumRatersParam", test3args("minNumRatersParam")).
      arg("maxNumRatersParam", test3args("maxNumRatersParam")).
      arg("minIntersectionParam", test3args("minIntersectionParam")).
      arg("minNumRatedSimParam", test3args("minNumRatedSimParam")).
      source(Tsv(DataFile(hdfsRoot, 1, 2, 3, None, "ratings.tsv")), test3Input).
      sink[(String, String, Double)](Tsv(AlgoFile(hdfsRoot, 1,2,3,None,"itemRecScores.tsv"))) { outputBuffer =>
         "correctly calculate itemRecScores" in {
           roundingData(outputBuffer.toList) must containTheSameElementsAs(roundingData(test3Output))
         }
       }
       .run
       .finish
       
  }
}