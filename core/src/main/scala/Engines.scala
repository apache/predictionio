package io.prediction

import io.prediction.core.BaseEngine
import io.prediction.core.Spark2LocalAlgorithm
import io.prediction.core.ParallelAlgorithm
import io.prediction.core.SingleAlgoEngine

class Spark2LocalSimpleEngine[TD, F, P](
  val algorithm: Class[_ <: Spark2LocalAlgorithm[TD, F, P, _, _ <: BaseParams]]
) extends SingleAlgoEngine (
  classOf[SparkDefaultCleanser[TD]],
  algorithm,
  classOf[DefaultServer[F, P]])

class ParallelSimpleEngine[TD, F, P](
  val algorithm: Class[_ <: ParallelAlgorithm[TD, F, P, _, _ <: BaseParams]]
) extends SingleAlgoEngine (
  classOf[SparkDefaultCleanser[TD]],
  algorithm,
  classOf[DefaultServer[F, P]])

