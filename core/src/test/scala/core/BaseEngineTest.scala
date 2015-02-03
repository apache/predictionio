package io.prediction.core

import org.specs2.mutable._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import n.io.prediction.workflow.SparkContextSetup

class BaseEngineSpec extends Specification {
  //System.clearProperty("spark.driver.port")
  //System.clearProperty("spark.hostPort")
  //val sc = new SparkContext("local[4]", "BaseEngineSpec test")
  //val sc = SparkContextSetup.sc

  "TestEngine " should {
    val te = new TestEngine()

    /*
    "XX" in {
      val v = te.train(sc)
      v must beEqualTo(Seq(1,2,3))
    }

    "YY" in {
      val v = te.test(sc)
      v.collect().toSeq must containTheSameElementsAs(Seq(5,3,1))
    }
    */
   
    /*
    "YY2" in {
      val v = te.test(sc)
      v.collect() must contain(exactly(5,3,1,2))
    }
    
    "YY3" in {
      val v = te.test(sc)
      v.collect() must contain(exactly(5,3,1,1))
    }
    */

  } 

}
