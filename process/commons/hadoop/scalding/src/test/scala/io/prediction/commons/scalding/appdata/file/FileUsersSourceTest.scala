package io.prediction.commons.scalding.appdata.file

import org.specs2.mutable._

import com.twitter.scalding._

class ReadUsersTest(args: Args) extends Job(args) {
  val appidArg: Int = args("appid").toInt
  val writeAppidArg: Int = args("writeAppid").toInt

  val src = new FileUsersSource("testpath", appidArg)

  src.readData('uid)
    .write(Tsv("output"))

  val writeDataSink = new FileUsersSource("writeDataTestpath", appidArg)

  src.readData('uid)
    .then ( writeDataSink.writeData('uid, writeAppidArg) _ )

  val writeObjSink = new FileUsersSource("writeObjTestpath", appidArg)
  src.readObj('user)
    .then ( writeObjSink.writeObj('user) _ )

}

class FileUsersSourceTest extends Specification with TupleConversions {

  def test(users: List[(String, String, String)]) = {//, expected: List[String]) = {

    val appid = 4
    val writeAppid = 3

    val expected = users map { case (id, appid, t) => id }   
    val writeDataExpected = users map { case (id, appid, t) => (id, writeAppid.toString)}    
    val writeObjExpected = users

    JobTest("io.prediction.commons.scalding.appdata.file.ReadUsersTest")
      .arg("appid", appid.toString)
      .arg("writeAppid", writeAppid.toString)
      .source(new FileUsersSource("testpath", appid.toInt), users)
      .sink[(String)](Tsv("output")) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(expected)
        }
      }
      .sink[(String, String)]((new FileUsersSource("writeDataTestpath", appid)).getSource) { outputBuffer =>
        "sink with writeData using different appid" in {
          outputBuffer must containTheSameElementsAs(writeDataExpected)
        }
        
      }
      .sink[(String, String, String)]((new FileUsersSource("writeObjTestpath", appid)).getSource) { outputBuffer =>
        "sink with writeObj" in {
          outputBuffer must containTheSameElementsAs(writeObjExpected)
        }
      }
      .run
      .finish
  }

  "FileUsersSourceTest" should {
    val testUsers = List(("u0", "4", "123456"), ("u1", "4", "23456"), ("u2", "4", "455677"), ("u3", "4", "876563111"))

    test(testUsers)

  }

}