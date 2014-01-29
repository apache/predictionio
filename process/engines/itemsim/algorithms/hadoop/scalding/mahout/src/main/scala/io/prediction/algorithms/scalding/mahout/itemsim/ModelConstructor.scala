package io.prediction.algorithms.scalding.mahout.itemsim

import com.twitter.scalding._

import io.prediction.commons.filepath.{DataFile, AlgoFile}
import io.prediction.commons.scalding.modeldata.ItemSimScores

/**
 * Source:
 *
 * Sink: 
 * 
 * Description:
 *   
 * Required args:
 * --dbType: <string> modeldata DB type (eg. mongodb) (see --dbHost, --dbPort)
 * --dbName: <string> (eg. predictionio_modeldata)
 * 
 * --hdfsRoot: <string>. Root directory of the HDFS
 * 
 * --appid: <int>
 * --engineid: <int>
 * --algoid: <int>
 * --modelSet: <boolean> (true/false). flag to indicate which set
 *
 * --numSimilarItems: <int>. number of similar items to be generated
 *
 * Optionsl args:
 * --dbHost: <string> (eg. "127.0.0.1")
 * --dbPort: <int> (eg. 27017)
 * 
 * --evalid: <int>. Offline Evaluation if evalid is specified
 * --debug: <String>. "test" - for testing purpose
 * 
 * Example:
 *
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
  val evalidArg = args.optional("evalid") map (x => x.toInt)
  val OFFLINE_EVAL = (evalidArg != None) // offline eval mode
    
  val debugArg = args.list("debug")
  val DEBUG_TEST = debugArg.contains("test") // test mode
  
  val modelSetArg = args("modelSet").toBoolean
  
  val numSimilarItems = args("numSimilarItems").toInt

  /**
   * source
   */
  val similarities = Tsv(AlgoFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "similarities.tsv"), ('iindex, 'simiindex, 'score)).read
    .mapTo(('iindex, 'simiindex, 'score) -> ('iindex, 'simiindex, 'score)) { 
      fields: (String, String, Double) => fields // convert score from String to Double
    }

  val itemsIndex = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "itemsIndex.tsv")).read
    .mapTo((0, 1, 2) -> ('iindexI, 'iidI, 'itypesI)) { fields: (String, String, String) =>
      val (iindex, iid, itypes) = fields // itypes are comma-separated String
      
      (iindex, iid, itypes.split(",").toList) 
    }
  
  /**
   * sink
   */

  val ItemSimScoresSink = ItemSimScores(dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg, algoid=algoidArg, modelset=modelSetArg)

  /**
   * computation
   */
  val sim = similarities.joinWithSmaller('iindex -> 'iindexI, itemsIndex)
    .discard('iindex, 'iindexI)
    .rename(('iidI, 'itypesI) -> ('iid, 'itypes))
    .joinWithSmaller('simiindex -> 'iindexI, itemsIndex)

  val sim1 = sim.project('iid, 'iidI, 'itypesI, 'score)
  val sim2 = sim.mapTo(('iidI, 'iid, 'itypes, 'score) -> ('iid, 'iidI, 'itypesI, 'score)) { fields: (String, String, List[String], Double) => fields }

  val combinedSimilarities = sim1 ++ sim2

  combinedSimilarities
    .groupBy('iid) { _.sortBy('score).reverse.toList[(String, Double, List[String])](('iidI, 'score, 'itypesI) -> 'simiidsList) }
    .then ( ItemSimScoresSink.writeData('iid, 'simiidsList, algoidArg, modelSetArg) _ )
  
}
