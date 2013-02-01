package io.prediction.commons.scalding.appdata.file

import org.specs2.mutable._

import com.twitter.scalding._

class ReadItypesTestJob(args: Args) extends Job(args) {
  
  val appidArg: Int = args("appid").toInt
  val itypesArg: List[String] = args.list("itypes")

  System.err.println(itypesArg)
  
  // use itypesArg.mkString(",").size instead of itypesArg.size
  // to work aroud empty List("") corner case.
  val queryItypes: Option[List[String]] = if ( itypesArg.mkString(",").length == 0) None else Some(itypesArg)
  
  val src = new FileItemsSource("testpath", appidArg, queryItypes)
  
  src.readData('iid, 'itypes)
    .mapTo(('iid, 'itypes) -> ('iid, 'itypes)) { fields: (String, List[String]) =>
      val (iid, itypes) = fields
      
      // during read, itypes are converted from t1,t2,t3 to List[String] = List(t1,t2,t3)
      // convert the List back to string with ',' as separator
      (iid, itypes.mkString(","))
    }.write(Tsv("output"))
    

}

/*
class WriteTestJob(args: Args) extends Job(args) {
  
  val src1 = new FileItemsSource(1, None)
  val src2 = new FileItemsSource(2, None)
  
  val p = src1.read
  
  src2.writeData(p)
}
*/

class FileItemsSourceTest extends Specification with TupleConversions {
  
  val test1Input = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
  val test1output_all = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i2", "t4"), ("i3", "t3,t4"))
  val test1output_t4 = List(("i2", "t4"), ("i3", "t3,t4"))
  val test1output_t2t3 = List(("i0", "t1,t2,t3"), ("i1", "t2,t3"), ("i3", "t3,t4"))
  val test1output_none = List()
  
  "FileItemsSource without itypes" should {
    JobTest("io.prediction.commons.scalding.appdata.file.ReadItypesTestJob").
      arg("appid", "1").
      source(new FileItemsSource("testpath", 1, None), test1Input).
      sink[(String, String)](Tsv("output")) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(test1output_all)
        }
      }.run.finish
  }

  "FileItemsSource with one itype" should {
    JobTest("io.prediction.commons.scalding.appdata.file.ReadItypesTestJob").
      arg("appid", "2").
      arg("itypes", List("t4")).
      source(new FileItemsSource("testpath", 2, Some(List("t4"))), test1Input).
      sink[(String, String)](Tsv("output")) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(test1output_t4)
        }
      }.run.finish
  }
    
  "FileItemsSource with some itypes" should {
    JobTest("io.prediction.commons.scalding.appdata.file.ReadItypesTestJob").
      arg("appid", "3").
      arg("itypes", List("t2","t3")).
      source(new FileItemsSource("testpath", 3, Some(List("t2","t3"))), test1Input).
      sink[(String, String)](Tsv("output")) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(test1output_t2t3)
        }
      }.run.finish
  }
  
  "FileItemsSource with all itypes" should {
    JobTest("io.prediction.commons.scalding.appdata.file.ReadItypesTestJob").
      arg("appid", "3").
      arg("itypes", List("t2","t3","t1","t4")).
      source(new FileItemsSource("testpath", 3, Some(List("t2","t3","t1","t4"))), test1Input).
      sink[(String, String)](Tsv("output")) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(test1output_all)
        }
      }.run.finish
  }
  
  "FileItemsSource without any matching itypes" should {
    JobTest("io.prediction.commons.scalding.appdata.file.ReadItypesTestJob").
      arg("appid", "3").
      arg("itypes", List("t99")).
      source(new FileItemsSource("testpath", 3, Some(List("t99"))), test1Input).
      sink[(String, String)](Tsv("output")) { outputBuffer =>
        "correctly read from a file" in {
          outputBuffer must containTheSameElementsAs(test1output_none)
        }
      }.run.finish
  }
  
  /*
  /**
   * write test
   */
  
  "Writing to FileItemsSource" should {
    JobTest("io.prediction.commons.scalding.appdata.file.WriteTestJob").
      source(new FileItemsSource(1, None), test1Input).
      sink[(String, String)](new FileItemsSource(2, None)) { outputBuffer =>
        "work" in {
          outputBuffer must containTheSameElementsAs(test1Input)
        }
      }.run.finish
  }
  */
}