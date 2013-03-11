package io.prediction.algorithms.mahout.itemrec.itembased

import com.twitter.scalding._

import io.prediction.commons.filepath.{DataFile, AlgoFile}
import io.prediction.commons.scalding.modeldata.ItemRecScores

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
 * --unseenOnly: <boolean> (true/false). only recommend unseen items if this is true. 
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
  
  val unseenOnlyArg = args("unseenOnly").toBoolean

  /**
   * source
   */
 
  val predicted = Tsv(AlgoFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "predicted.tsv"), ('uindex, 'predicted)).read

  val ratingSource = Csv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "ratings.csv"), ",", ('uindex, 'iindex, 'rating))

  val itemsIndex = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "itemsIndex.tsv")).read
    .mapTo((0, 1, 2) -> ('iindexI, 'iidI, 'itypesI)) { fields: (String, String, String) =>
      val (iindex, iid, itypes) = fields // itypes are comma-separated String
      
      (iindex, iid, itypes.split(",").toList) 
    }
  
  val usersIndex = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "usersIndex.tsv")).read
    .mapTo((0, 1) -> ('uindexU, 'uidU)) { fields: (String, String) =>

    fields
  }

  /**
   * sink
   */

  val itemRecScoresSink = ItemRecScores(dbType=dbTypeArg, dbName=dbNameArg, dbHost=dbHostArg, dbPort=dbPortArg)

  /**
   * computation
   */

  val predictedRating = predicted.flatMap('predicted -> ('iindex, 'rating)) { data: String => parsePredictedData(data) }
    .project('uindex, 'iindex, 'rating)

  val combinedRating = if (unseenOnlyArg) predictedRating else (predictedRating ++ (ratingSource.read))
  
  combinedRating
    // just in case, if there are duplicates for the same u-i pair, simply take 1
    // But note this is not supposed to happen anyway, because
    // Mahout only recommends items which are not in ratings set.
    .groupBy('uindex, 'iindex) { _.take(1) }
    .joinWithSmaller('iindex -> 'iindexI, itemsIndex)
    .joinWithSmaller('uindex -> 'uindexU, usersIndex)
    .then( itemRecScoresSink.writeData('uidU, 'iidI, 'rating, 'itypesI, algoidArg, modelSetArg) _ )
  
  /*
  Mahout ItemRec output format
  [24:3.2] => (24, 3.2)
  [8:2.5,0:2.5]  => (8, 2.5), (0, 2.5)
  [0:2.0]
  [16:3.0]
  */
  def parsePredictedData(data: String) : List[(String, String)] = {
    val dataLen = data.length
    data.take(dataLen-1).tail.split(",").toList.map{ ratingData => 
      val ratingDataArray = ratingData.split(":")
      (ratingDataArray(0), ratingDataArray(1))
    }
  }
}
