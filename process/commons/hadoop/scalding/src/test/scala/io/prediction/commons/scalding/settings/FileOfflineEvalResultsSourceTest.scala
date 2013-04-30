package io.prediction.commons.scalding.settings.file

import org.specs2.mutable._

import com.twitter.scalding._

class FileOfflineEvalResultsSourceWriteTestJob(args: Args) extends Job(args) {

  val resultsSink = new FileOfflineEvalResultsSource("testpath")

  Tsv("FileOfflineEvalResultsSourceTestInput").read
    .mapTo((0,1,2,3,4) -> ('evalid, 'metricid, 'algoid, 'score, 'iteration)) { 
      fields: (Int, Int, Int, Double, Int) =>

      fields
    }
    .then ( resultsSink.writeData('evalid, 'metricid, 'algoid, 'score, 'iteration) )

}

class FileOfflineEvalResultsSourceTest extends Specification with TupleConversions {
  
  def test(testInput: List[(Int, Int, Int, Double, Int)]) = {

    JobTest("io.prediction.commons.scalding.settings.file.FileOfflineEvalResultsSourceWriteTestJob")
      .source(Tsv("FileOfflineEvalResultsSourceTestInput"), testInput)
      .sink[(Int, Int, Int, Double, Int)](new FileOfflineEvalResultsSource("testpath")) { outputBuffer =>
        "correctly write to FileOfflineEvalResultsSource" in {
          outputBuffer.toList must containTheSameElementsAs(testInput)
        }
      }
      .run
      .finish
  }

  "FileOfflineEvalResultsSourceWriteTestJob" should {
    val test1Input = List((6, 2, 3, 0.123, 5))
    test(test1Input)

    val test2Input = List((11, 2, 5, 0.444, 6))
    test(test2Input)
  }
}