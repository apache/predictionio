package io.prediction

import io.prediction.core.BaseEngine
import io.prediction.core.Spark2LocalAlgorithm

// Simple Engine has only one algo
class Spark2LocalSimpleEngine[TD, F, P](
  val algorithm: Class[_ <: Spark2LocalAlgorithm[TD, F, P, _, _]]
) extends BaseEngine(
  classOf[SparkDefaultCleanser[TD]],
  Map("" -> algorithm),
  classOf[DefaultServer[F, P]])
