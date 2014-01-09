package io.prediction.commons.scalding.settings

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.flow.FlowDef

/**
 * OfflineEvalResultsSource
 */
trait OfflineEvalResultsSource {

  import com.twitter.scalding.Dsl._ // get all the fancy implicit conversions that define the DSL

  def getSource: Source

  def writeData(evalidField: Symbol, metricidField: Symbol, algoidField: Symbol, scoreField: Symbol, iterationField: Symbol, splitsetField: Symbol)(p: Pipe)(implicit fd: FlowDef): Pipe

}

object OfflineEvalResultsSource {

  val FIELD_SYMBOLS: Map[String, Symbol] = Map(
    ("id" -> 'id),
    ("evalid" -> 'evalid),
    ("metricid" -> 'metricid),
    ("algoid" -> 'algoid),
    ("score" -> 'score),
    ("iteration" -> 'iteration),
    ("splitset" -> 'splitset))

}