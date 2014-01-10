package io.prediction.commons.scalding.modeldata.file

import org.specs2.mutable._

import com.twitter.scalding._

class FileItemRecScoresSourceWriteTestJob(args: Args) extends Job(args) {

  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = args.optional("evalid") map (x => x.toInt)
  val modelSetArg = args("modelSet").toBoolean

  val itemRecSink = new FileItemRecScoresSource("testpath")

  Tsv("FileItemRecScoresSourceTestInput").read
    .mapTo((0, 1, 2, 3) -> ('uid, 'iid, 'score, 'itypes)) { fields: (String, String, Double, String) =>
      val (uid, iid, score, itypes) = fields

      (uid, iid, score, itypes.split(",").toList)
    }
    .then(itemRecSink.writeData('uid, 'iid, 'score, 'itypes, algoidArg, modelSetArg) _)

}

class FileItemRecScoresSourceReadTestJob(args: Args) extends Job(args) {

  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = args.optional("evalid") map (x => x.toInt)
  val modelSetArg = args("modelSet").toBoolean

  val itemRecSource = new FileItemRecScoresSource("testpath")

  itemRecSource.readData('uid, 'iid, 'score, 'itypes)
    .mapTo(('uid, 'iid, 'score, 'itypes) -> ('uid, 'iid, 'score, 'itypes)) { fields: (String, String, Double, List[String]) =>
      val (uid, iid, score, itypes) = fields

      (uid, iid, score, itypes.mkString(","))
    }
    .write(Tsv("FileItemRecScoresSourceTestOutput"))

}

class FileItemRecScoresSourceTest extends Specification with TupleConversions {

  "FileItemRecScoresSourceWriteTestJob" should {
    val appid = 4
    val engineid = 3
    val algoid = 18
    val evalid: Option[Int] = None
    val modelSet: Boolean = false

    val test1Input = List(("u0", "i1", "0.7", "t1,t2,t3"), ("u0", "i2", "0.44", "t1"), ("u1", "i2", "0.1", "t4"))
    val test1Output = List(("u0", "i1", 0.7, "t1,t2,t3", algoid, modelSet), ("u0", "i2", 0.44, "t1", algoid, modelSet), ("u1", "i2", 0.1, "t4", algoid, modelSet))

    JobTest("io.prediction.commons.scalding.modeldata.file.FileItemRecScoresSourceWriteTestJob")
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      .source(Tsv("FileItemRecScoresSourceTestInput"), test1Input)
      .sink[(String, String, Double, String, Int, Boolean)](new FileItemRecScoresSource("testpath")) { outputBuffer =>
        "correctly write to FileItemRecScoresSource" in {
          outputBuffer.toList must containTheSameElementsAs(test1Output)
        }

      }
      .run
      .finish
  }

  "FileItemRecScoresSourceReadTestJob" should {
    val appid = 4
    val engineid = 3
    val algoid = 18
    val evalid: Option[Int] = None
    val modelSet: Boolean = false

    val test1Input = List(("u0", "i1", "0.7", "t1,t2,t3"), ("u0", "i2", "0.44", "t1"), ("u1", "i2", "0.1", "t4"))
    val test1Output = List(("u0", "i1", 0.7, "t1,t2,t3"), ("u0", "i2", 0.44, "t1"), ("u1", "i2", 0.1, "t4"))

    JobTest("io.prediction.commons.scalding.modeldata.file.FileItemRecScoresSourceReadTestJob")
      .arg("appid", appid.toString)
      .arg("engineid", engineid.toString)
      .arg("algoid", algoid.toString)
      .arg("modelSet", modelSet.toString)
      .source(new FileItemRecScoresSource("testpath"), test1Input)
      .sink[(String, String, Double, String)](Tsv("FileItemRecScoresSourceTestOutput")) { outputBuffer =>
        "correctly write to FileItemRecScoresSource" in {
          outputBuffer.toList must containTheSameElementsAs(test1Output)
        }

      }
      .run
      .finish
  }

}
