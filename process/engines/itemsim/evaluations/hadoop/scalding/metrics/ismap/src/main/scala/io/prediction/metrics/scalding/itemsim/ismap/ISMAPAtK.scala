package io.prediction.metrics.scalding.itemsim.ismap

import com.twitter.scalding._

import cascading.pipe.joiner.LeftJoin
import cascading.pipe.joiner.RightJoin

import io.prediction.commons.filepath.OfflineMetricFile
import io.prediction.commons.scalding.settings.OfflineEvalResults

/**
 * Source:
 *   relevantUsers.tsv
 *     iid     uid
 *     i0      u0
 *     i0      u1
 *     i0      u2
 *   relevantItems.tsv
 *     uid     iid
 *     u0      i0
 *     u0      i1
 *     u0      i2
 *   topKItems.tsv
 *     iid     simiid  score
 *     i0      i1      3.2
 *     i0      i4      2.5
 *     i0      i5      1.4
 *
 * Sink:
 *   offlineEvalResults DB
 *   averagePrecision.tsv
 *     iid     ap
 *     i0      0.03
 *
 *
 * Description:
 *   Calculate Item Similarity Mean Average Precision @ k score
 *   There is an assumption that the number of missing similar items per item is always equal to k.
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
 * scald.rb --hdfs-local io.prediction.metrics.scalding.itemsim.ismap.ISMAPAtK --dbType mongodb --dbName predictionio --dbHost 127.0.0.1 --dbPort 27017 --hdfsRoot hdfs/predictionio/ --appid 34 --engineid 3 --evalid 15 --metricid 10 --algoid 9 --kParam 30
 */
class ISMAPAtK(args: Args) extends Job(args) {
  /** parse args */
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
  val iterationArg = args.getOrElse("iteration", "1").toInt
  val splitsetArg = args.getOrElse("splitset", "")

  val kParamArg = args("kParam").toInt

  val debugArg = args.list("debug")
  val DEBUG_TEST = debugArg.contains("test") // test mode

  /** sources */
  val relevantUsers = TypedTsv[(String, String)](OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "relevantUsers.tsv"), ('ruiid, 'ruuid))
  val relevantItems = TypedTsv[(String, String)](OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "relevantItems.tsv"), ('riuid, 'riiid))
  val relevantItemsAsList = relevantItems.groupBy('riuid) { _.toList[String]('riiid -> 'riiids) }
  val topKItems = TypedTsv[(String, String, Double)](OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "topKItems.tsv"), ('iid, 'simiid, 'score))

  /** sinks */
  val averagePrecisionSink = Tsv(OfflineMetricFile(hdfsRootArg, appidArg, engineidArg, evalidArg, metricidArg, algoidArg, "averagePrecision.tsv"))
  val offlineEvalResultsSink = OfflineEvalResults(dbType = dbTypeArg, dbName = dbNameArg, dbHost = dbHostArg, dbPort = dbPortArg)

  /** computation */
  val itemsMapAtK = topKItems
    .joinWithSmaller('iid -> 'ruiid, relevantUsers)
    .joinWithSmaller('ruuid -> 'riuid, relevantItemsAsList, joiner = new LeftJoin)
    .groupBy('iid, 'ruuid) {
      _.sortBy('score).reverse.scanLeft(('simiid, 'riiids) -> ('precision, 'hit, 'count))((0.0, 0, 0)) {
        (newFields: (Double, Int, Int), fields: (String, List[String])) =>
          val (simiid, riiids) = fields
          val (precision, hit, count) = newFields
          Option(riiids) map { r =>
            if (r.contains(simiid)) {
              ((hit + 1).toDouble / (count + 1).toDouble, hit + 1, count + 1)
            } else {
              (0.0, hit, count + 1)
            }
          } getOrElse {
            (0.0, hit, count + 1)
          }
      }
    }
    .filter('count) { count: Int => count > 0 }
    .groupBy('iid, 'ruuid) { _.average('precision) }
    .groupBy('iid) { _.average('precision) }

  itemsMapAtK.write(averagePrecisionSink)

  itemsMapAtK.groupAll { _.average('precision) }
    .mapTo('precision -> ('evalid, 'metricid, 'algoid, 'score, 'iteration, 'splitset)) { precision: Double =>
      (evalidArg, metricidArg, algoidArg, precision, iterationArg, splitsetArg)
    }
    .then(offlineEvalResultsSink.writeData('evalid, 'metricid, 'algoid, 'score, 'iteration, 'splitset) _)
}
