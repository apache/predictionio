package io.prediction.commons.scalding.modeldata.file

import org.specs2.mutable._

import com.twitter.scalding._

class FileItemSimScoresSourceTestJob(args: Args) extends Job(args) {
    
  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = args.optional("evalid") map (x => x.toInt)
  val modelSetArg = args("modelSet").toBoolean
  
  val itemSimSink = new FileItemSimScoresSource("testpath")
  
  Tsv("FileItemSimScoresSourceTestInput").read
    .mapTo((0, 1, 2, 3) -> ('iid, 'simiid, 'score, 'simitypes)) { fields: (String, String, Double, String) =>
      val (iid, simiid, score, simitypes) = fields
      
      (iid, simiid, score, simitypes.split(",").toList)
    }
    .then( itemSimSink.writeData('iid, 'simiid, 'score, 'simitypes, algoidArg, modelSetArg) _ )

}

class FileItemSimScoresSourceTest extends Specification with TupleConversions {
  
  "FileItemSimScoresSourceTestJob" should {
    val appid = 4
    val engineid = 3
    val algoid = 18
    val evalid: Option[Int] = None
    val modelSet: Boolean = true
    
    val test1Input = List(("i0", "i1", "0.7", "t1,t2,t3"), ("i0", "i1", "0.44", "t1"),  ("i1", "i2", "0.1", "t4"))
    val test1Output = List(("i0", "i1", 0.7, "t1,t2,t3", algoid, modelSet), ("i0", "i1", 0.44, "t1", algoid, modelSet),  ("i1", "i2", 0.1, "t4", algoid, modelSet))
    
    JobTest("io.prediction.commons.scalding.modeldata.file.FileItemSimScoresSourceTestJob")
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      .source(Tsv("FileItemSimScoresSourceTestInput"), test1Input)
      .sink[(String, String, Double, String, Int, Boolean)](new FileItemSimScoresSource("testpath")) { outputBuffer =>
        "correctly write to FileItemSimScoresSource" in {
          outputBuffer.toList must containTheSameElementsAs(test1Output)
        }
      }
      .run
      .finish
  }
  
}


