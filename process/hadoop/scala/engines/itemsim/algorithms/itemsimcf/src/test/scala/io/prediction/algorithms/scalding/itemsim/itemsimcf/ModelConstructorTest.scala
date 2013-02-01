package io.prediction.algorithms.scalding.itemsim.itemsimcf

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{AlgoFile, DataFile}
import io.prediction.commons.scalding.modeldata.ItemSimScores

class ModelConstructorTest extends Specification with TupleConversions {
  "ItemSim ModelConstructor in test mode" should {
    val appid = 3
    val engineid = 4
    val algoid = 7
    val modelSet = true
    val test1ItemSimScores = List(("i0", "i1", "0.123"), ("i0", "i2", "0.456"))
    val test1Items = List(("i0", "t1,t2,t3"), ("i1", "t1,t2"), ("i2", "t2,t3"))
    val test1Output = List(("i0", "i1", 0.123, "t1,t2", algoid, modelSet), ("i0", "i2", 0.456, "t2,t3", algoid, modelSet))

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None
    val dbPort = None
    val hdfsRoot = "testroot/"
    
    JobTest("io.prediction.algorithms.scalding.itemsim.itemsimcf.ModelConstructor")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      //.arg("debug", "test") // NOTE: test mode
      .source(Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, None, "itemSimScores.tsv")), test1ItemSimScores)
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, None, "selectedItems.tsv")), test1Items)
      .sink[(String, String, Double, String, Int, Boolean)](ItemSimScores(dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort).getSource) { outputBuffer =>
        "correctly write model data to a file" in {
          outputBuffer.toList must containTheSameElementsAs(test1Output)
        }
    }
    .run
    .finish
  }
}
