package io.prediction.commons.scalding.appdata.file

import org.specs2.mutable._

import com.twitter.scalding._

class ReadUsersTest(args: Args) extends Job(args) {
  val appidArg: Int = args("appid").toInt

  val src = new FileUsersSource("testpath", appidArg)

  src.readData('uid)
    .write(Tsv("output"))

}

class FileUsersSourceTest extends Specification with TupleConversions {

  def test(users: List[(String, String)], expected: List[String]) = {

    val appid = "3"

    JobTest("io.prediction.commons.scalding.appdata.file.ReadUsersTest")
      .arg("appid", appid)
      .source(new FileUsersSource("testpath", appid.toInt), users)
      .sink[(String)](Tsv("output")) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(expected)
        }
      }
      .run
      .finish
  }

  "FileUsersSourceTest" should {
    val testUsers = List(("u0", "4"), ("u1", "4"), ("u2", "4"), ("u3", "4"))
    val expected = List("u0", "u1", "u2", "u3")

    test(testUsers, expected)

  }

}