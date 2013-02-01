package io.prediction.commons.scalding.appdata.mongodb.examples

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.mongodb.{MongoItemsSource, MongoU2iActionsSource}

//TODO: clean up this example. see if there is better way to test MongoSource?
class ReadWrite(args: Args) extends Job(args) {

  /**
   * test MongoItemsSource
   * read from DB and write to Tsv
   */
  val itemsSource = new MongoItemsSource("appdata", "127.0.0.1", 27017, 12, None)
    
  val items = itemsSource.readData('iid, 'itypes)
    .write(Tsv("items.tsv"))
  
  val testItems = new MongoItemsSource("test_appdata", "127.0.0.1", 27017, 10, None)
  items.then( testItems.writeData('iid, 'itypes, 10) _ ) 
  
  /**
   * test MongoU2iActionsSource
   * read from DB and write to Tsv
   */
  val u2iSource = new MongoU2iActionsSource("appdata", "127.0.0.1", 27017, 12)

  val u2i = u2iSource.readData('action, 'uid, 'iid, 't, 'v)
    u2i.write(Tsv("u2iData.tsv"))
    
  val testU2i = new MongoU2iActionsSource("test_appdata", "127.0.0.1", 27017, 10)
  u2i.then( testU2i.writeData('action, 'uid, 'iid, 't, 'v, 10) _ )
  
      
  
}
