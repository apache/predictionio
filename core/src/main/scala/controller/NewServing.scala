package n.io.prediction.controller

//import io.prediction.core.BaseAlgorithm
import n.io.prediction.core.BaseServing
import io.prediction.core.AbstractDoer

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._
import scala.reflect.runtime.universe._

/** Base class of serving. 
  *
  * @tparam Q Input query class.
  * @tparam P Output prediction class.
  * @group Serving
  */
abstract class LServing[Q, P] extends BaseServing[Q, P] {
  def serveBase(q: Q, ps: Seq[P]): P = {
    serve(q, ps)
  }

  /** Implement this method to combine multiple algorithms' predictions to
    * produce a single final prediction.
    *
    * @param query Input query.
    * @param predictions A list of algorithms' predictions.
    */
  def serve(query: Q, predictions: Seq[P]): P
}
