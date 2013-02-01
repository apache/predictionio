package io.prediction.evaluations.scalding.itemrec.trainingtestsplit

import com.twitter.scalding._

import io.prediction.commons.scalding.appdata.{Items, U2iActions}

/**
 * Source:
 *   appdata.items
 *   appdata.u2iActions
 *   
 * Sink:
 *   training_appdata.u2iActions
 *   test_appdata.u2iAcions
 *   
 * Description:
 *   Split the appata u2iActions into Training and Test set for ItemRec engine
 *   
 * Args:
 * --dbType: <string> appdata DB type
 * --dbName: <string>
 * --dbHost: <string>. optional. (eg. "127.0.0.1")
 * --dbPort: <int>. optional. (eg. 27017)
 * 
 * --training_dbType: <string> training_appadta DB type
 * --training_dbName: <string>
 * --training_dbHost: <string>. optional
 * --training_dbPort: <int>. optional
 * 
 * --test_dbType: <string> test_appdata DB type
 * --test_dbName: <string>
 * --test_dbHost: <string>. optional
 * --test_dbPort: <int>. optional
 * 
 * --appid: <int>
 * --engineid: <int>
 * --evalid: <int>
 * --itypes: <string separated by white space>. eg "--itypes type1 type2". If no --itypes specified, then ALL itypes will be used.
 * 
 * --trainingsize: <int> (1 - 10)
 * --testsize: <int> (1 - 10)
 * 
 * Example:
 * scald.rb --hdfs-local io.prediction.evaluations.scalding.itemrec.trainingtestsplit.TrainingTestSplit --dbType mongodb --dbName appdata --dbHost 127.0.0.1 --dbPort 27017 --appid 34 --engineid 3 --evalid 15 --itypes t2 --trainingsize 8 --testsize 2  --training_dbType mongodb --training_dbName training_appdata --training_dbHost 127.0.0.1 --training_dbPort 27017 --test_dbType mongodb --test_dbName test_appdata --test_dbHost 127.0.0.1 --test_dbPort 27017
 * 
 */
class TrainingTestSplit(args: Args) extends Job(args) {
  
  /**
   * parse arguments
   */
  val dbTypeArg = args("dbType")
  val dbNameArg = args("dbName")
  val dbHostArg = args.optional("dbHost")
  val dbPortArg = args.optional("dbPort") map (x => x.toInt) 
  
  val training_dbTypeArg = args("training_dbType")
  val training_dbNameArg = args("training_dbName")
  val training_dbHostArg = args.optional("training_dbHost")
  val training_dbPortArg = args.optional("training_dbPort") map (x => x.toInt) 
  
  val test_dbTypeArg = args("test_dbType")
  val test_dbNameArg = args("test_dbName")
  val test_dbHostArg = args.optional("test_dbHost")
  val test_dbPortArg = args.optional("test_dbPort") map (x => x.toInt) 
  
  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val evalidArg = args("evalid").toInt
  
  val preItypesArg = args.list("itypes")
  val itypesArg: Option[List[String]] = if (preItypesArg.mkString(",").length == 0) None else Option(preItypesArg)

  val trainingsizeArg = args("trainingsize").toInt
  val testsizeArg = args("testsize").toInt
  
  val totalSize = trainingsizeArg + testsizeArg
  require((totalSize <= 10), "The total of trainingsize and testsize must be <= 10")
  
  /**
   * source
   */
  // get appdata
  val items = Items(appId=appidArg, itypes=itypesArg,
      dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg).readData('iidx, 'itypes)
  
  val u2i = U2iActions(appId=appidArg,
      dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg).readData('action, 'uid, 'iid, 't, 'v)
  
  /**
   * sink
   */
  // sink to training_appdata
  // NOTE: appid is replaced by evalid for training and test set appdata
  val trainingItems = Items(appId=evalidArg, itypes=None, 
      dbType=training_dbTypeArg, dbName=training_dbNameArg, dbHost=training_dbHostArg, dbPort=training_dbPortArg)
      
  val trainingU2i = U2iActions(appId=evalidArg,
      dbType=training_dbTypeArg, dbName=training_dbNameArg, dbHost=training_dbHostArg, dbPort=training_dbPortArg)
  
  // sink to test_appadta
  val testU2i = U2iActions(appId=evalidArg,
      dbType=test_dbTypeArg, dbName=test_dbNameArg, dbHost=test_dbHostArg, dbPort=test_dbPortArg)
     
  /**
   * computation
   */
    
  val oldPrefix: String = appidArg + "_"
  val newPrefix: String = evalidArg + "_"
  
  val selectedU2i = u2i.joinWithSmaller('iid -> 'iidx, items) // only select actions of these items
    .map(('uid, 'iid) -> ('randValue, 'newUid, 'newIid)) { fields: (String, String) =>
      
      // NOTE: replace appid prefix by evalid
      val (uid, iid) = fields
      val newUid = newPrefix + uid.stripPrefix(oldPrefix)
      val newIid = newPrefix + iid.stripPrefix(oldPrefix)
      
      // scala.math.random is evenly distributed
      val r = (scala.math.random * 10).toInt
      
      (r, newUid, newIid)
    }
  
  selectedU2i.filter('randValue) { r: Int => (r < testsizeArg)}
    .then( testU2i.writeData('action, 'newUid, 'newIid, 't, 'v, evalidArg) _ ) // NOTE: appid is repalced by evalid 
    
  selectedU2i.filter('randValue) { r: Int => ((r >= testsizeArg) && (r < totalSize)) }
    .then( trainingU2i.writeData('action, 'newUid, 'newIid, 't, 'v, evalidArg) _) // NOTE: appid is repalced by evalid 
  
  items.map('iidx -> 'newIid) {iid: String =>
    val newIid = newPrefix + iid.stripPrefix(oldPrefix)
    newIid
  }.then( trainingItems.writeData('newIid, 'itypes, evalidArg) _ ) // NOTE: appid is repalced by evalid 
  
    
}