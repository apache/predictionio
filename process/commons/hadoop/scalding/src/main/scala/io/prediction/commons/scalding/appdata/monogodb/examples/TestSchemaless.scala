package io.prediction.commons.scalding.appdata.mongodb.examples

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.mongodb.{ MongoUsersSource, MongoItemsSource, MongoU2iActionsSource }
import io.prediction.commons.appdata.{ Item, User }

class TestSchemaless(args: Args) extends Job(args) {

  val read_dbNameArg = args("read_dbName")
  val read_dbHostArg = args("read_dbHost")
  val read_dbPortArg = args("read_dbPort").toInt

  val read_appidArg = args("read_appid").toInt

  val preItypesArg = args.list("itypes")
  val itypesArg: Option[List[String]] = if (preItypesArg.mkString(",").length == 0) None else Option(preItypesArg)

  val itemsSource = new MongoItemsSource(read_dbNameArg, read_dbHostArg, read_dbPortArg, read_appidArg, itypesArg)

  val itemsStarttime = itemsSource.readStartEndtime('iid, 'itypes, 'starttime, 'endtime)
    .mapTo(('iid, 'itypes, 'starttime, 'endtime) -> ('iid, 'itypes, 'starttime, 'endtime)) {
      fields: (String, List[String], Long, Option[Long]) =>
        (fields._1, fields._2.mkString(","), fields._3, fields._4.getOrElse("PIO_NONE"))
    }
    .write(Tsv("itemsStarttime.tsv"))

}

class TestSchemaless2(args: Args) extends Job(args) {

  Tsv("itemsStarttime.tsv").read
    .mapTo((0, 1, 2, 3) -> ('id, 'itypes, 'starttime, 'endtime)) {
      fields: (String, String, Long, String) =>
        val endtime: Option[Long] = fields._4 match {
          case "PIO_NONE" => None
          case x: String => Some(x.toLong)
        }

        (fields._1, fields._2, fields._3, endtime)
    }
    .write(Tsv("itemsStarttime2.tsv"))
    .filter('endtime) { x: Option[Long] => x != None }
    .write(Tsv("itemsStarttime3.tsv"))

}