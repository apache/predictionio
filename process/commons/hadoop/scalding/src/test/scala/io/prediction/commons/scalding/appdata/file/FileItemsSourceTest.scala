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
  
  src.readStarttime('iid, 'itypes, 'starttime)
    .mapTo(('iid, 'itypes, 'starttime) -> ('iid, 'itypes, 'starttime)) { fields: (String, List[String], String) =>
      val (iid, itypes, starttime) = fields
      
      // during read, itypes are converted from t1,t2,t3 to List[String] = List(t1,t2,t3)
      // convert the List back to string with ',' as separator
      (iid, itypes.mkString(","), starttime)
    }.write(Tsv("outputStarttime"))
  
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
  
  val test1Input = List(("i0", "t1,t2,t3", "12345"), ("i1", "t2,t3", "45678"), ("i2", "t4", "9"), ("i3", "t3,t4", "101"))
  val test1output_all = List(("i0", "t1,t2,t3", "12345"), ("i1", "t2,t3", "45678"), ("i2", "t4", "9"), ("i3", "t3,t4", "101"))
  val test1output_t4 = List(("i2", "t4", "9"), ("i3", "t3,t4", "101"))
  val test1output_t2t3 = List(("i0", "t1,t2,t3", "12345"), ("i1", "t2,t3", "45678"), ("i3", "t3,t4", "101"))
  val test1output_none = List()


  def getIidAndItypes(d: List[(String, String, String)]) = {
    d map {x => (x._1, x._2)}
  }
  
  def testWithItypes(appid: Int, itypes: List[String], inputItems: List[(String, String, String)], outputItems: List[(String, String, String)]) = {
    JobTest("io.prediction.commons.scalding.appdata.file.ReadItypesTestJob")
      .arg("appid", appid.toString)
      .arg("itypes", itypes)
      .source(new FileItemsSource("testpath", appid, Some(itypes)), inputItems)
      .sink[(String, String)](Tsv("output")) { outputBuffer =>
        "correctly read iid and itypes" in {
          outputBuffer must containTheSameElementsAs(getIidAndItypes(outputItems))
        }
      }
      .sink[(String, String, String)](Tsv("outputStarttime")) { outputBuffer =>
        "correctly read starttime" in {
          outputBuffer must containTheSameElementsAs(outputItems)
        }
      }.run.finish
  }

  def testWithoutItypes(appid: Int, inputItems: List[(String, String, String)], outputItems: List[(String, String, String)]) = {
    JobTest("io.prediction.commons.scalding.appdata.file.ReadItypesTestJob")
      .arg("appid", appid.toString)
      .source(new FileItemsSource("testpath", appid, None), inputItems)
      .sink[(String, String)](Tsv("output")) { outputBuffer =>
        "correctly read iid and itypes" in {
          outputBuffer must containTheSameElementsAs(getIidAndItypes(outputItems))
        }
      }
      .sink[(String, String, String)](Tsv("outputStarttime")) { outputBuffer =>
        "correctly read starttime" in {
          outputBuffer must containTheSameElementsAs(outputItems)
        }
      }.run.finish
  }


  "FileItemsSource without itypes" should {
    testWithoutItypes(1, test1Input, test1output_all)
  }

  "FileItemsSource with one itype" should {
    testWithItypes(1, List("t4"), test1Input, test1output_t4)
  }
    
  "FileItemsSource with some itypes" should {
    testWithItypes(3, List("t2","t3"), test1Input, test1output_t2t3)
  }
  
  "FileItemsSource with all itypes" should {
    testWithItypes(3, List("t2","t3","t1","t4"), test1Input, test1output_all)
  }
  
  "FileItemsSource without any matching itypes" should {
    testWithItypes(3,List("t99"), test1Input, test1output_none)
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