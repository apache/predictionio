package io.prediction.algorithms.scalding.itemrec.knnitembased

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{ AlgoFile, DataFile }
import io.prediction.commons.scalding.modeldata.ItemRecScores

class ModelConstructorTest extends Specification with TupleConversions {
  "itemrec.knnitembased ModelConstructor in test mode" should {
    val appid = 3
    val engineid = 4
    val algoid = 7
    val modelSet = true
    val test1ItemRecScores = List(("u0", "i1", "0.123"), ("u0", "i2", "0.456"), ("u1", "i0", "1.23"))
    val test1Items = List(("i0", "t1,t2,t3"), ("i1", "t1,t2"), ("i2", "t2,t3"))
    val test1Output = List(("u0", "i2,i1", "0.456,0.123", "[t2,t3],[t1,t2]", algoid, modelSet), ("u1", "i0", "1.23", "[t1,t2,t3]", algoid, modelSet))

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None
    val dbPort = None
    val hdfsRoot = "testroot/"

    JobTest("io.prediction.algorithms.scalding.itemrec.knnitembased.ModelConstructor")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      //.arg("debug", "test") // NOTE: test mode
      .source(Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, None, "itemRecScores.tsv")), test1ItemRecScores)
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, None, "selectedItems.tsv")), test1Items)
      .sink[(String, String, String, String, Int, Boolean)](ItemRecScores(dbType = dbType, dbName = dbName, dbHost = dbHost, dbPort = dbPort, algoid = algoid, modelset = modelSet).getSource) { outputBuffer =>
        "correctly write model data to a file" in {
          outputBuffer.toList must containTheSameElementsAs(test1Output)
        }
      }
      .run
      .finish
  }
}
