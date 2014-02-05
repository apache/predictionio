package io.prediction.commons.scalding.appdata.mongodb.examples

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.mongodb.{ MongoUsersSource, MongoItemsSource, MongoU2iActionsSource }
import io.prediction.commons.appdata.{ Item, User }

//TODO: clean up this example. see if there is better way to test MongoSource?
class ReadWrite(args: Args) extends Job(args) {

  val read_dbNameArg = args("read_dbName")
  val read_dbHostArg = args("read_dbHost")
  val read_dbPortArg = args("read_dbPort").toInt

  val read_appidArg = args("read_appid").toInt

  val write_dbNameArg = args("write_dbName")
  val write_dbHostArg = args("write_dbHost")
  val write_dbPortArg = args("write_dbPort").toInt

  val write_appidArg = args("write_appid").toInt

  val preItypesArg = args.list("itypes")
  val itypesArg: Option[List[String]] = if (preItypesArg.mkString(",").length == 0) None else Option(preItypesArg)

  /**
   * test MongoUsersSource
   * read from DB and write to Tsv
   */
  val usersSource = new MongoUsersSource(read_dbNameArg, read_dbHostArg, read_dbPortArg, read_appidArg)

  val users = usersSource.readData('uid)
    .write(Tsv("users.tsv"))

  val usersObj = usersSource.readObj('user)
    .write(Tsv("usersObj.tsv"))

  val testUsersObj = new MongoUsersSource(write_dbNameArg + "_obj", write_dbHostArg, write_dbPortArg, write_appidArg)

  usersObj.mapTo('user -> 'user) { obj: User =>
    obj.copy(appid = write_appidArg)
  }.then(testUsersObj.writeObj('user) _)

  val testUsers = new MongoUsersSource(write_dbNameArg, write_dbHostArg, write_dbPortArg, write_appidArg)
  users.then(testUsers.writeData('uid, write_appidArg) _)

  /**
   * test MongoItemsSource
   * read from DB and write to Tsv
   */
  val itemsSource = new MongoItemsSource(read_dbNameArg, read_dbHostArg, read_dbPortArg, read_appidArg, itypesArg)

  val items = itemsSource.readData('iid, 'itypes)
    .write(Tsv("items.tsv"))

  val itemsStarttime = itemsSource.readStartEndtime('iid, 'itypes, 'starttime, 'endtime)
    .write(Tsv("itemsStarttime.tsv"))

  val itemsObj = itemsSource.readObj('item)
    .write(Tsv("itemsObj.tsv"))

  val testItemsObj = new MongoItemsSource(write_dbNameArg + "_obj", write_dbHostArg, write_dbPortArg, write_appidArg, itypesArg)

  itemsObj.mapTo('item -> 'item) { obj: Item =>
    obj.copy(appid = write_appidArg)
  }.then(testItemsObj.writeObj('item) _)

  val testItems = new MongoItemsSource(write_dbNameArg, write_dbHostArg, write_dbPortArg, write_appidArg, itypesArg)
  items.then(testItems.writeData('iid, 'itypes, write_appidArg) _)

  /**
   * test MongoU2iActionsSource
   * read from DB and write to Tsv
   */
  val u2iSource = new MongoU2iActionsSource(read_dbNameArg, read_dbHostArg, read_dbPortArg, read_appidArg)

  val u2i = u2iSource.readData('action, 'uid, 'iid, 't, 'v)
  u2i.write(Tsv("u2iData.tsv"))

  val testU2i = new MongoU2iActionsSource(write_dbNameArg, write_dbHostArg, write_dbPortArg, write_appidArg)
  u2i.then(testU2i.writeData('action, 'uid, 'iid, 't, 'v, write_appidArg) _)

}
