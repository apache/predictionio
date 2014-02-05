package io.prediction.commons.scalding.appdata.file

import org.specs2.mutable._

import com.twitter.scalding._

class ReadItypesTestJob(args: Args) extends Job(args) {

  val appidArg: Int = args("appid").toInt
  val writeAppidArg: Int = args("writeAppid").toInt

  val preItypesArg = args.list("itypes")
  // use itypesArg.mkString(",").size instead of itypesArg.size
  // to work aroud empty List("") corner case.
  val itypesArg: Option[List[String]] = if (preItypesArg.mkString(",").length == 0) None else Option(preItypesArg)

  System.err.println(itypesArg)

  val src = new FileItemsSource("testpath", appidArg, itypesArg)

  src.readData('iid, 'itypes)
    .mapTo(('iid, 'itypes) -> ('iid, 'itypes)) { fields: (String, List[String]) =>
      val (iid, itypes) = fields

      // during read, itypes are converted from t1,t2,t3 to List[String] = List(t1,t2,t3)
      // convert the List back to string with ',' as separator
      (iid, itypes.mkString(","))
    }.write(Tsv("output"))

  src.readStartEndtime('iid, 'itypes, 'starttime, 'endtime)
    .mapTo(('iid, 'itypes, 'starttime, 'endtime) -> ('iid, 'itypes, 'starttime, 'endtime)) { fields: (String, List[String], Long, Option[Long]) =>
      val (iid, itypes, starttime, endtime) = fields

      // during read, itypes are converted from t1,t2,t3 to List[String] = List(t1,t2,t3)
      // convert the List back to string with ',' as separator
      (iid, itypes.mkString(","), starttime.toString, endtime.map(_.toString).getOrElse("PIO_NONE"))
    }.write(Tsv("outputStartEndtime"))

  val writeDataSink = new FileItemsSource("writeDataTestpath", appidArg, None)

  src.readData('iid, 'itypes)
    .then(writeDataSink.writeData('iid, 'itypes, writeAppidArg) _)

  val writeObjSink = new FileItemsSource("writeObjTestpath", appidArg, None)

  src.readObj('item)
    .then(writeObjSink.writeObj('item) _)
}

class FileItemsSourceTest extends Specification with TupleConversions {

  val test1Input = List(
    ("i0", "t1,t2,t3", "appid", "2293300", "1266673", "666554320"),
    ("i1", "t2,t3", "appid", "14526361", "12345135", "PIO_NONE"),
    ("i2", "t4", "appid", "14526361", "23423424", "PIO_NONE"),
    ("i3", "t3,t4", "appid", "1231415", "378462511", "666554323"))

  val test1output_all = test1Input

  val test1output_t4 = List(
    ("i2", "t4", "appid", "14526361", "23423424", "PIO_NONE"),
    ("i3", "t3,t4", "appid", "1231415", "378462511", "666554323"))

  val test1output_t2t3 = List(
    ("i0", "t1,t2,t3", "appid", "2293300", "1266673", "666554320"),
    ("i1", "t2,t3", "appid", "14526361", "12345135", "PIO_NONE"),
    ("i3", "t3,t4", "appid", "1231415", "378462511", "666554323"))

  val test1output_none = List()

  def testWithItypes(appid: Int, writeAppid: Int, itypes: List[String],
    inputItems: List[(String, String, String, String, String, String)],
    outputItems: List[(String, String, String, String, String, String)]) = {

    val inputSource = inputItems map { case (id, itypes, tempAppid, starttime, ct, endtime) => (id, itypes, appid.toString, starttime, ct, endtime) }
    val outputExpected = outputItems map { case (id, itypes, tempAppid, starttime, ct, endtime) => (id, itypes) }
    val outputStartEndtimeExpected = outputItems map { case (id, itypes, tempAppid, starttime, ct, endtime) => (id, itypes, starttime, endtime) }
    val writeDataExpected = outputItems map { case (id, itypes, tempAppid, starttime, ct, endtime) => (id, itypes, writeAppid.toString) }
    val writeObjExpected = outputItems map { case (id, itypes, tempAppid, starttime, ct, endtime) => (id, itypes, appid.toString, starttime, ct) }

    JobTest("io.prediction.commons.scalding.appdata.file.ReadItypesTestJob")
      .arg("appid", appid.toString)
      .arg("writeAppid", writeAppid.toString)
      .arg("itypes", itypes)
      .source(new FileItemsSource("testpath", appid, Some(itypes)), inputSource)
      .sink[(String, String)](Tsv("output")) { outputBuffer =>
        "correctly read iid and itypes" in {
          outputBuffer must containTheSameElementsAs(outputExpected)
        }
      }
      .sink[(String, String, String, String)](Tsv("outputStartEndtime")) { outputBuffer =>
        "correctly read starttime" in {
          outputBuffer must containTheSameElementsAs(outputStartEndtimeExpected)
        }
      }
      .sink[(String, String, String)]((new FileItemsSource("writeDataTestpath", appid, None)).getSource) { outputBuffer =>
        "sink with writeData using different appid" in {
          outputBuffer must containTheSameElementsAs(writeDataExpected)
        }
      }
      .sink[(String, String, String, String, String)]((new FileItemsSource("writeObjTestpath", appid, None)).getSource) { outputBuffer =>
        "sink with writeObj" in {
          outputBuffer must containTheSameElementsAs(writeObjExpected)
        }
      }
      .run.finish
  }

  "FileItemsSource without itypes" should {
    testWithItypes(1, 3, List(""), test1Input, test1output_all)
  }

  "FileItemsSource with one itype" should {
    testWithItypes(1, 5, List("t4"), test1Input, test1output_t4)
  }

  "FileItemsSource with some itypes" should {
    testWithItypes(3, 6, List("t2", "t3"), test1Input, test1output_t2t3)
  }

  "FileItemsSource with all itypes" should {
    testWithItypes(3, 7, List("t2", "t3", "t1", "t4"), test1Input, test1output_all)
  }

  "FileItemsSource without any matching itypes" should {
    testWithItypes(3, 10, List("t99"), test1Input, test1output_none)
  }

}