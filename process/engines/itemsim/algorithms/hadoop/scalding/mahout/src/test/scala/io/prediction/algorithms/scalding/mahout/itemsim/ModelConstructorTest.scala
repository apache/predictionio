package io.prediction.algorithms.scalding.mahout.itemsim

import org.specs2.mutable._

import com.twitter.scalding._

import io.prediction.commons.filepath.{AlgoFile, DataFile}
import io.prediction.commons.scalding.modeldata.ItemSimScores
import cascading.tuple.{Tuple, TupleEntry, TupleEntryIterator, Fields}

class ModelConstructorTest extends Specification with TupleConversions {

  def test(numSimilarItems: Int,
    items: List[(String, String, String)],
    similarities: List[(String, String, String)],
    output: List[(String, String, String, String)]) = {

    val appid = 3
    val engineid = 4
    val algoid = 7
    val evalid = None
    val modelSet = true

    val dbType = "file"
    val dbName = "testpath/"
    val dbHost = None
    val dbPort = None
    val hdfsRoot = "testroot/"
    
    val itemSimScores = output map { case (iid, simiid, score, simitypes) => (iid, simiid, score, simitypes, algoid, modelSet)} 

    JobTest("io.prediction.algorithms.scalding.mahout.itemsim.ModelConstructor")
      .arg("dbType", dbType)
      .arg("dbName", dbName)
      .arg("hdfsRoot", hdfsRoot)
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      .arg("numSimilarItems", numSimilarItems.toString)
      .source(Tsv(AlgoFile(hdfsRoot, appid, engineid, algoid, evalid, "similarities.tsv"), new Fields("iindex", "simiindex", "score")), similarities)
      .source(Tsv(DataFile(hdfsRoot, appid, engineid, algoid, evalid, "itemsIndex.tsv")), items)
      .sink[(String, String, String, String, Int, Boolean)](ItemSimScores(dbType=dbType, dbName=dbName, dbHost=dbHost, dbPort=dbPort, algoid=algoid, modelset=modelSet).getSource) { outputBuffer =>
        "correctly write model data to a file" in {
          outputBuffer.toList must containTheSameElementsAs(itemSimScores)
        }
    }
    .run
    .finish

  }

  val test1Items = List(("0", "i0", "t1,t2,t3"), ("1", "i1", "t1,t2"), ("2", "i2", "t2,t3"), ("3", "i3", "t2"))
    
  val test1Similarities = List( 
    ("0", "1", "0.83"),
    ("0", "2", "0.25"),
    ("0", "3", "0.49"),
    ("1", "2", "0.51"),
    ("1", "3", "0.68"),
    ("2", "3", "0.32"))

  val test1Output = List(
    ("i0", "i1,i3,i2", "0.83,0.49,0.25", "[t1,t2],[t2],[t2,t3]"),
    ("i1", "i0,i3,i2", "0.83,0.68,0.51", "[t1,t2,t3],[t2],[t2,t3]"),
    ("i2", "i1,i3,i0", "0.51,0.32,0.25", "[t1,t2],[t2],[t1,t2,t3]"),
    ("i3", "i1,i0,i2", "0.68,0.49,0.32", "[t1,t2],[t1,t2,t3],[t2,t3]"))

  "mahout.itemsim ModelConstructor" should {

    test(100, test1Items, test1Similarities, test1Output)

  }

  val test2Items = List(("0", "i0", "t1,t2,t3"), ("1", "i1", "t1,t2"), ("2", "i2", "t2,t3"), ("3", "i3", "t2"))
    
  val test2Similarities = List( 
    ("0", "1", "83"),
    ("0", "2", "200"),
    ("0", "3", "4"),
    ("1", "2", "9"),
    ("1", "3", "68"),
    ("2", "3", "1000"))

  val test2Output = List(
    ("i0", "i2,i1,i3", "200.0,83.0,4.0", "[t2,t3],[t1,t2],[t2]"),
    ("i1", "i0,i3,i2", "83.0,68.0,9.0", "[t1,t2,t3],[t2],[t2,t3]"),
    ("i2", "i3,i0,i1", "1000.0,200.0,9.0", "[t2],[t1,t2,t3],[t1,t2]"),
    ("i3", "i2,i1,i0", "1000.0,68.0,4.0", "[t2,t3],[t1,t2],[t1,t2,t3]"))

  "mahout.itemsim ModelConstructor (score should not be compared as string)" should {

    test(100, test2Items, test2Similarities, test2Output)

  }

}
