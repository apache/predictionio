package io.prediction.algorithms.scalding.itemsim.itemsimcf

import com.twitter.scalding._

import cascading.pipe.Pipe

import io.prediction.commons.filepath.{ DataFile, AlgoFile }

/**
 * Source: ratings.tsv
 * Sink: itemSimScores.tsv
 * Descripton:
 *   Compute item similarity score.
 *
 * Required args:
 * --hdfsRoot: <string>. Root directory of the HDFS
 *
 * --appid: <int>
 * --engineid: <int>
 * --algoid: <int>
 *
 * --measureParam: <string>. distance measurement function. select one of "correl", "cosine", "jaccard"
 * --priorCountParam: <int>. for regularization. number of virtual pairs
 * --priorCorrelParam: <double>. for regularization. correlation of these virtual pairs
 * --minNumRatersParam: <int>. min number of raters of the item
 * --maxNumRatersParam: <int> max number of raters of the item
 * --minIntersectionParam: <int>. min number of co-rater users between 2 simliar items
 * --numSimilarItems: <int>. number of similar items to be generated
 *
 * Optional args:
 * --evalid: <int>. Offline Evaluation if evalid is specified
 *
 * Example:
 * scald.rb --hdfs-local io.prediction.algorithms.scalding.itemsim.itemsimcf.ItemSimilarity --hdfsRoot hdfs/predictionio/ --appid 34 --engineid 2 --algoid 8 --measureParam correl --priorCountParam 20 --priorCorrelParam 0.05
 */
class ItemSimilarity(args: Args) extends VectorSimilarities(args) {

  // args
  val hdfsRootArg = args("hdfsRoot")

  val appidArg = args("appid").toInt
  val engineidArg = args("engineid").toInt
  val algoidArg = args("algoid").toInt
  val evalidArg = args.optional("evalid") map (x => x.toInt)

  val measureParamArg = args("measureParam")
  val priorCountParamArg = args("priorCountParam").toInt
  val priorCorrelParamArg = args("priorCorrelParam").toDouble

  val minNumRatersParamArg = args("minNumRatersParam").toInt
  val maxNumRatersParamArg = args("maxNumRatersParam").toInt
  val minIntersectionParamArg = args("minIntersectionParam").toInt
  val numSimilarItemsArg = args("numSimilarItems").toInt

  // override VectorSimilarities param
  override val MEASURE: String = measureParamArg

  override val PRIOR_COUNT: Int = priorCountParamArg

  override val PRIOR_CORRELATION: Double = priorCorrelParamArg

  override val MIN_NUM_RATERS: Int = minNumRatersParamArg

  override val MAX_NUM_RATERS: Int = maxNumRatersParamArg

  override val MIN_INTERSECTION: Int = minIntersectionParamArg

  override def input(userField: Symbol, itemField: Symbol, ratingField: Symbol): Pipe = {
    Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "ratings.tsv")).read
      .mapTo((0, 1, 2) -> (userField, itemField, ratingField)) { fields: (String, String, Double) => fields }

  }

  // start computation
  vectorSimilaritiesAlgo('iid, 'simiid, 'score)
    .groupBy('iid) { _.sortBy('score).reverse.take(numSimilarItemsArg) }
    .write(Tsv(AlgoFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "itemSimScores.tsv")))

}
