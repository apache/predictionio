package io.prediction.commons.scalding.appdata.file

import org.specs2.mutable._

import com.twitter.scalding._

//import io.prediction.commons.scalding.AppDataFile

class ReadU2iActionsTestJob(args: Args) extends Job(args) {

  val appidArg: Int = args("appid").toInt

  val src = new FileU2iActionsSource("testpath", appidArg)

  src.readData('action, 'uid, 'iid, 't, 'v)
    .mapTo(('action, 'uid, 'iid, 't, 'v) -> ('action, 'uid, 'iid, 't, 'v)) {
      fields: (String, String, String, String, Option[String]) =>
        val (action, uid, iid, t, v) = fields

        (action, uid, iid, t, v.getOrElse("PIO_NONE"))
    }
    .write(Tsv("output"))

}

class WriteU2iActionsTestJob(args: Args) extends Job(args) {

  val appidArg: Int = args("appid").toInt

  val src = new FileU2iActionsSource("testpath", appidArg)
  val sink = new FileU2iActionsSource("testpathwr", appidArg)

  src.readData('action, 'uid, 'iid, 't, 'v)
    .then(sink.writeData('action, 'uid, 'iid, 't, 'v, appidArg) _)

}

class FileU2iActionsSourceTest extends Specification with TupleConversions {
  // action: String// 0
  // uid: String // 1
  // iid: String // 2
  // t: String // 3
  // v: String // 4

  val test1Input = List(("rate", "uid3", "iid5", "12345", "5"), ("view", "uid2", "iid6", "12346", "PIO_NONE"))
  val appid = 1

  "ReadU2iActionsTest" should {
    JobTest("io.prediction.commons.scalding.appdata.file.ReadU2iActionsTestJob")
      .arg("appid", appid.toString)
      .source(new FileU2iActionsSource("testpath", appid), test1Input)
      .sink[(String, String, String, String, String)](Tsv("output")) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(test1Input)
        }
      }.run.finish
  }

  "WriteU2iActionsTest" should {
    JobTest("io.prediction.commons.scalding.appdata.file.WriteU2iActionsTestJob")
      .arg("appid", appid.toString)
      .source(new FileU2iActionsSource("testpath", appid), test1Input)
      .sink[(String, String, String, String, String)]((new FileU2iActionsSource("testpathwr", appid)).getSource) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(test1Input)
        }
      }.run.finish
  }
}