package io.prediction.algorithms.scalding.itemrec.knnitembased

import com.twitter.scalding._
import com.twitter.scalding.mathematics.Matrix
import io.prediction.commons.filepath.{DataFile, AlgoFile}

import cascading.pipe.Pipe

/**
 * Source: ratings.tsv
 * Sink: itemRecScores.tsv
 * Descripton:
 *   predict and compute the scores of every predictable user-item pair.
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
 * --minNumRatedSimParam: <int>. minimum number of rated similar items for valid prediction
 * 
 * Optional args:
 * --evalid: <int>. Offline Evaluation if evalid is specified
 * --mergeRatingParam: If defined, merge known rating into final predicted scores
 * 
 * Example:
 * scald.rb --hdfs-local io.prediction.algorithms.scalding.itemrec.knnitembased.KNNItemBased --hdfsRoot hdfs/predictionio/ --appid 34 --engineid 3 --algoid 9 --measureParam correl --priorCountParam 20 --priorCorrelParam 0.05 --minNumRatersParam 1 --maxNumRatersParam 10000 --minIntersectionParam 1 --minNumRatedSimParam 1
 */
class KNNItemBased(args: Args) extends VectorSimilarities(args) {
  
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
  val minNumRatedSimParamArg = args("minNumRatedSimParam").toInt
    
  val mergeRatingParamArg = args.boolean("mergeRatingParam") // true if it's defined.
  
  // override VectorSimilarities param
  override val MEASURE: String = measureParamArg
  
  override val PRIOR_COUNT: Int = priorCountParamArg
  
  override val PRIOR_CORRELATION: Double = priorCorrelParamArg
  
  override val MIN_NUM_RATERS: Int = minNumRatersParamArg 
  
  override val MAX_NUM_RATERS: Int = maxNumRatersParamArg
  
  override val MIN_INTERSECTION: Int = minIntersectionParamArg
  
  val ratingsRaw = Tsv(DataFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "ratings.tsv")).read
  
  override def input(userField : Symbol, itemField : Symbol, ratingField : Symbol): Pipe = {
    ratingsRaw
      .mapTo((0, 1, 2) -> (userField, itemField, ratingField)) {fields: (String, String, Double) => fields}
    
  }
  
  // TODO: may add feature to load intermediate itemSimScores result without
  // computing vectorSimilaritiesAlgo() again
  
  val MIN_NUM_RATED_SIMILAR: Int = minNumRatedSimParamArg
  
  val PREDICT_ONLY: Boolean = !mergeRatingParamArg
  
  // start computation  
  val ratings = ratingsRaw.mapTo((0, 1, 2) -> ('uid, 'iid, 'rating)) {fields: (String, String, Double) => fields}
  
  vectorSimilaritiesAlgo('iid, 'simiid, 'score)//.write(Tsv(AlgoFile(engineidArg, algoidArg, evalidArg, "itemSimScores.tsv").path))
    .then( weightedSumAlgo(ratings, MIN_NUM_RATED_SIMILAR, PREDICT_ONLY) _ )
    .write(Tsv(AlgoFile(hdfsRootArg, appidArg, engineidArg, algoidArg, evalidArg, "itemRecScores.tsv")))


  /*
   * Weighted Sum Algo
   * parameters
   * 	itemSimScores Pipe: 'iid 'simiid 'score
   * 	ratings Pipe: 'uid 'iid 'rating
   * returns
   * 	itemRecScores Pipe: 'uid 'iid 'score
   */
  def weightedSumAlgo(ratings: Pipe, minNumRatedSimiid: Int=1, predictOnly: Boolean=false)(itemSimScores: Pipe): Pipe = {
    
    import Matrix._
    
    // predict user u's rating of item i = sum(rating of u-j * score of i-j) / sum(abs(score of i-j)) 
    // where j is all similar items of item i. score of i-j is the similarity score between i and j.
    // rating of u-j is the known rating of item j rated by user u.
  
    // convert rating and itemSimScores to matrix
    // construct a uid x iid matrix
    val ratingsMat = ratings.toMatrix[String, String, Double]('uid, 'iid, 'rating)
  
    // construct a simiid x iid matrx
    val itemSimScoresMat = itemSimScores.toMatrix[String, String, Double]('simiid, 'iid, 'score)
  
    // matrix dot product
    // this has the numerator value for each u-i
    val weightedRatingSumMat = (ratingsMat * itemSimScoresMat)

    // make all exisiting rating become 1
    val ratingsBinMat = ratingsMat.binarizeAs[Double]
  
    // calculate the absolute value of itemSimScores score
    val absItemsimscoresMat = itemSimScoresMat.mapValues{ score: Double => if (score < 0) -score else score}
 
    // this is the denomator of the prediction formular
    val absScoreSumMat = (ratingsBinMat * absItemsimscoresMat)
  
    val predictScoreMat = weightedRatingSumMat.elemWiseOp(absScoreSumMat) { (x, y) => x/y }
    
    // do filtering with number of rated simiid
    val itemSimScoresBinMat = itemSimScoresMat.binarizeAs[Double] // setting non-zero itemSimScores to 1
    
    val numRatedSimiidMat = (ratingsBinMat * itemSimScoresBinMat) // the number of rated simiid for each uid-iid pair
    
    // create matrix of ((numRatedSimiid, predictScore), rating)
    // eg. ((0, 0), x1). no prediction but with known rating
    //     ((x1, x2), x3). with prediction of x2 (note: x2 could be 0) and known rating of x3
    //     ((x1, x2), 0). with prediction of x2 (note: x2 could be 0) and no known rating
    
    val predictMat = numRatedSimiidMat.zip(predictScoreMat).zip(ratingsMat)
    
    // convert back to Pipe
    val predict = predictMat.pipeAs('uid, 'iid, 'data)
         .mapTo(('uid, 'iid, 'data) -> ('uid, 'iid, 'numRatedSimiid, 'predict, 'rating)) {fields: (String, String, ((Double, Double), Double)) =>
           val (uid, iid, ((numRatedSimiid, predict), rating)) = fields
           
           (uid, iid, numRatedSimiid, predict, rating)
    }
   
    val itemRecScores = predict.filter('numRatedSimiid, 'predict, 'rating) { fields: (Double, Double, Double) =>
      val (numRatedSimiid, predict, rating) = fields
      
      (numRatedSimiid >= minNumRatedSimiid) || ((!predictOnly) && (rating != 0))
        
    }.mapTo(('uid, 'iid, 'numRatedSimiid, 'predict, 'rating) -> ('uid, 'iid, 'score)) { fields: (String, String, Double, Double, Double) =>
      val (uid, iid, numRatedSimiid, predict, rating) = fields
      
      val score = if ((!predictOnly) && (rating != 0)) rating else predict
        
      (uid, iid, score)
    }
    
    itemRecScores    
  }

  
}