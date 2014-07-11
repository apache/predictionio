package io.prediction

import io.prediction.core.BaseEngine
import io.prediction.core.Spark2LocalAlgorithm
import io.prediction.core.ParallelAlgorithm
import io.prediction.core.SingleAlgoEngine

import org.json4s.Formats

class Spark2LocalSimpleEngine[TD, F, P](
  val algorithm: Class[_ <: Spark2LocalAlgorithm[TD, F, P, _, _ <: BaseParams]],
  formats: Formats = Util.json4sDefaultFormats
  ) extends SingleAlgoEngine (
  classOf[SparkDefaultCleanser[TD]],
  algorithm,
  classOf[DefaultServer[F, P]],
  formats)

class ParallelSimpleEngine[TD, F, P](
  val algorithm: Class[_ <: ParallelAlgorithm[TD, F, P, _, _ <: BaseParams]],
  formats: Formats = Util.json4sDefaultFormats
  ) extends SingleAlgoEngine (
  classOf[SparkDefaultCleanser[TD]],
  algorithm,
  classOf[DefaultServer[F, P]],
  formats)
