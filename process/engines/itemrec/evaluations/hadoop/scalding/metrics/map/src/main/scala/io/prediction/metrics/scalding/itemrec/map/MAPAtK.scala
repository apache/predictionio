package io.prediction.metrics.scalding.itemrec.map

import com.twitter.scalding._

import cascading.pipe.joiner.LeftJoin
import cascading.pipe.joiner.RightJoin

import io.prediction.commons.filepath.OfflineMetricFile
import io.prediction.commons.scalding.settings.OfflineEvalResults

/**
 * Source:
 *   relevantItems.tsv eg  u0   i0,i1,i2
 *   topKItems.tsv  eg.  u0  i1,i4,i5
 *
 * Sink:
 *   offlineEvalResults DB
 *   averagePrecision.tsv  eg.  u0  0.03
 *
 *
 * Description:
 *   Calculate Mean Average Precision @ k score
 *
 * Required args:
 * --dbType: <string> The OfflineEvalResults DB Type (eg. mongodb) (see --dbHost, --dbPort)
 * --dbName: <string>
 *
 * --hdfsRoot: <string>. Root directory of the HDFS
 *
 * --appid: <int>
 * --engineid: <int>
 * --evalid: <int>
 * --metricid: <int>
 * --algoid: <int>
 * --iteration: <int>
 * --splitset: <string>
 *
 * --kParam: <int>
 *
 * Optional args:
 * --dbHost: <string> (eg. "127.0.0.1")
 * --dbPort: <int> (eg. 27017)
 *
 * --debug: <String>. "test" - for testing purpose
 *
 * Example:
 * scald.rb --hdfs-local io.prediction.metrics.scalding.itemrec.map.MAPAtK --dbType mongodb --dbName predictionio --dbHost 127.0.0.1 --dbPort 27017 --hdfsRoot hdfs/predictionio/ --appid 34 --engineid 3 --evalid 15 --metricid 10 --algoid 9 --kParam 30
 */
class MAPAtK(args: Args) extends Job(args) {

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
  val evalidArg = args("evalid").toInt
  val metricidArg = args("metricid").toInt
  val algoidArg = args("algoid").toInt
  //val iterationArg = args("iteration").toInt
  val iterationArg = args.getOrElse("iteration", "1").toInt
  val splitsetArg = args.getOrElse("splitset", "")

  val kParamArg = args("kParam").toInt

  val debugArg = args.list("debug")
  val DEBUG_TEST = debugArg.contains("test") // test mode

  /**
   * get Sources
   */
  val relevantItems = Tsv(OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "relevantItems.tsv")).read
    .mapTo((0, 1) -> ('uidTest, 'relevantList)) { fields: (String, String) =>
      val (uidTest, relevantList) = fields

      (uidTest, relevantList.split(",").toList)
    }

  val topKItems = Tsv(OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "topKItems.tsv")).read
    .mapTo((0, 1) -> ('uid, 'topList)) { fields: (String, String) =>
      val (uid, topList) = fields
      (uid, topList.split(",").toList)
    }

  /**
   * sink
   */

  val averagePrecisionSink = Tsv(OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "averagePrecision.tsv"))

  val offlineEvalResultsSink = OfflineEvalResults(dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg)

  /**
   * computation
   */

  // for each user, calculate the AP based on relvantList and topList

  // use RightJoin, so that if there is no topList but there is relevantList, set AP to 0.
  // if the user has no relevantList in relevantItems, ignore this user.
  val averagePrecision = topKItems.joinWithSmaller('uid -> 'uidTest, relevantItems, joiner = new RightJoin)
    .rename(('uid, 'topList) -> ('uidX, 'topListX))
    .map(('uidX, 'topListX, 'uidTest) -> ('uid, 'topList)) { fields: (String, List[String], String) =>
      val (uidX, topListX, uidTest) = fields

      if (uidX == null)
        (uidTest, List(""))
      else
        (uidX, topListX)
    }
    .map(('topList, 'relevantList) -> ('avgPreAtK, 'key, 'zeroAP, 'numOfHits)) { fields: (List[String], List[String]) =>
      val (topList, relevantList) = fields

      val (ap, numOfHits) = averagePrecisionAtK(kParamArg, topList, relevantList)
      val zeroAP = if (ap == 0) 1 else 0

      (ap, 1, zeroAP, numOfHits)
    }

  averagePrecision
    .mapTo(('uid, 'topList, 'relevantList, 'avgPreAtK, 'numOfHits) -> ('uid, 'topList, 'relevantList, 'avgPreAtK, 'numOfHits)) {
      fields: (String, List[String], List[String], Double, Int) =>
        val (uid, topList, relevantList, avgPreAtK, numOfHits) = fields

        (uid, topList.mkString(","), relevantList.mkString(","), avgPreAtK, numOfHits)
    }

  averagePrecision.project('uid, 'avgPreAtK, 'numOfHits)
    .write(averagePrecisionSink)

  val results = averagePrecision.groupBy('key) {
    _
      .size('num) // how many users in total
      .sum('avgPreAtK -> 'avgPreAtKSum)
      .sum('zeroAP -> 'numOfZeroAP)
  }
    .mapTo(('num, 'avgPreAtKSum, 'numOfZeroAP) -> ('evalid, 'metricid, 'algoid, 'score, 'iteration, 'splitset, 'num, 'numOfZeroAP)) { fields: (Int, Double, Int) =>

      val (num, avgPreAtKSum, numOfZeroAP) = fields

      val meanAveragePrecision = avgPreAtKSum / num

      (evalidArg, metricidArg, algoidArg, meanAveragePrecision, iterationArg, splitsetArg, num, numOfZeroAP)
    }

  results.then(offlineEvalResultsSink.writeData('evalid, 'metricid, 'algoid, 'score, 'iteration, 'splitset) _)

  /**
   * Calculate the average precision @ k
   *
   * ap@k = sum(P(i)/min(m, k)) wher i=1 to k
   * k is number of prediction to be retrieved.
   * P(i) is the precision at position i of the top-K list
   *    if the item at position i is relevant, then P(i) = (the number of releavent items up to that position in the top-k list / position)
   *    if the item at position i is not relevant, then P(i)=0
   * m is the number of relevant items for this user.
   *
   * return:
   *   averagePrecision (Double)
   *   numOfHits (Int)
   */
  def averagePrecisionAtK(k: Int, predictedItems: List[String], relevantItems: List[String]): (Double, Int) = {

    // supposedly the predictedItems.size should match k
    // NOTE: what if predictedItems is less than k? use the avaiable items as k.
    require((predictedItems.size <= k), "The size of predicted Items list should be <= k.")
    val n = scala.math.min(predictedItems.size, k)

    // find if each element in the predictedItems is one of the relevant items
    // if so, map to 1. else map to 0
    // (0, 1, 0, 1, 1, 0, 0)
    val relevantBinary: List[Int] = predictedItems.map { x => if (relevantItems.contains(x)) 1 else 0 }
    val numOfHits: Int = relevantBinary.sum

    // prepare the data for AP calculation.
    // take the relevantBinary List and map each element to tuple (num, index)
    // where num is number of 1s up to that position if the element is 1 and 0 if the elemtn is 0.
    // index is the position index of the element starting from 1.
    def prepareAPData(org: List[Int], accum: Int, index: Int): List[(Int, Int)] = {
      if (org.isEmpty)
        List()
      else {
        val newAccum = accum + org.head
        val num = if (org.head == 1) newAccum else 0
        val element = (num -> index)
        List(element) ++ prepareAPData(org.tail, newAccum, index + 1)
      }
    }

    val averagePrecisionData = prepareAPData(relevantBinary, 0, 1)

    val apDenom = scala.math.min(n, relevantItems.size)

    // NOTE: if relevantItems.size is 0, the averagePrecision is 0
    val averagePrecision = if (apDenom == 0) 0 else
      ((averagePrecisionData.map { x => if (x._1 != 0) (x._1.toDouble / x._2) else 0 }.sum) / apDenom)

    (averagePrecision, numOfHits)
  }

}