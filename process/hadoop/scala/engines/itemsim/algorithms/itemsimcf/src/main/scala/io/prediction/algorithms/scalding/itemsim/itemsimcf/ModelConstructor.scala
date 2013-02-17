package io.prediction.algorithms.scalding.itemsim.itemsimcf

import com.twitter.scalding._

import io.prediction.commons.filepath.{DataFile, AlgoFile}
import io.prediction.commons.scalding.modeldata.ItemSimScores

/**
 * Source: 
 *   selectedItems.tsv
 *   itemSimScores.tsv
 * Sink: 
 *   itemSimScores DB
 * Description:
 *   Read the itemSimScores.tsv and get additional attributes from selectedItems.tsv for each similiar items.
 *   Then write the result to model DB.
 *   
 * Required args:
 * --dbType: <string> modeldata DB type (eg. mongodb) (see --dbHost, --dbPort)
 * --dbName: <string>
 * 
 * --hdfsRoot: <string>. Root directory of the HDFS
 * 
 * --appid: <int>
 * --engineid: <int>
 * --algoid: <int>
 * --modelSet: <boolean> (true/false). flag to indicate which set
 * 
 * Optionsl args:
 * --dbHost: <string> (eg. "127.0.0.1")
 * --dbPort: <int> (eg. 27017)
 * 
 * --debug: <String>. "test" - for testing purpose
 * 
 * Example:
 * scald.rb --hdfs-local io.prediction.algorithms.scalding.itemsim.itemsimcf.ModelConstructor --dbType mongodb --dbName modeldata --dbHost 127.0.0.1 --dbPort 27017 --hdfsRoot hdfs/predictionio/ --appid 34 --engineid 2 --algoid 8 --modelSet false
 */
class ModelConstructor(args: Args) extends Job(args) {
  
  /**
   * parse args
   */
  val dbTypeArg = args("dbType")
  val dbNameArg = args("dbName")
  val dbHostArg = args.optional("dbHost")
  val dbPortArg = args.optional("dbPort") map (x => x.toInt)
  
  val hdfsRootArg = args("hdfsRoot")
  
  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = None //args.optional("evalid") map (x => x.toInt)
  val OFFLINE_EVAL = (evalidArg != None) // offline eval mode
  
  val debugArg = args.list("debug")
  val DEBUG_TEST = debugArg.contains("test") // test mode
  
  val modelSetArg = args("modelSet").toBoolean
  
  /**
   * input
   */
  val score = Tsv(AlgoFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "itemSimScores.tsv")).read
    .mapTo((0, 1, 2) -> ('iid, 'simiid, 'score)) { fields: (String, String, Double) => fields }
  
  val items = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "selectedItems.tsv")).read
    .mapTo((0, 1) -> ('iidx, 'itypes)) { fields: (String, String) =>
      val (iidx, itypes) = fields // itypes are comma-separated String
      
      (iidx, itypes.split(",").toList) 
    }
  
  /**
   * process & output
   */
  val p = score.joinWithSmaller('simiid -> 'iidx, items) // get items info for each simiid
    
  val src = ItemSimScores(dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg)
  
  p.then( src.writeData('iid, 'simiid, 'score, 'itypes, algoidArg, modelSetArg) _ )
  
}
