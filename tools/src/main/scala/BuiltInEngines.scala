package io.prediction.tools
import io.prediction.core.BuildInfo

case class EngineInstanceTemplate(
  val engineId: String,
  val version: String,
  val name: String,
  val engineFactory: String,
  val dataSourceJson: Any,
  val preparatorJson: Any,
  val algorithmsJson: Any,
  val servingJson: Any) {

  lazy val template = Map(
    "engine.json" -> 
        templates.scala.txt.engineJson(engineId, version, name, engineFactory),
    Console.joinFile(Seq("params", "datasource.json")) -> dataSourceJson,
    Console.joinFile(Seq("params", "preparator.json")) -> preparatorJson,
    Console.joinFile(Seq("params", "algorithms.json")) -> algorithmsJson,
    Console.joinFile(Seq("params", "serving.json")) -> servingJson)
}

object BuiltInEngine {
  // This variable defines all built-in engines. Append new engines to this list.
  val instances: Seq[EngineInstanceTemplate] = Seq(
    EngineInstanceTemplate(
      engineId = "io.prediction.engines.itemrank",
      version = BuildInfo.version,
      name = "PredictionIO ItemRank Engine",
      engineFactory = "io.prediction.engines.itemrank.ItemRankEngine",
      dataSourceJson = templates.itemrank.params.txt.datasourceJson(),
      preparatorJson = templates.itemrank.params.txt.preparatorJson(),
      algorithmsJson = templates.itemrank.params.txt.algorithmsJson(),
      servingJson = templates.itemrank.params.txt.servingJson()),
    EngineInstanceTemplate(
      engineId = "io.prediction.engines.itemrec",
      version = BuildInfo.version,
      name = "PredictionIO ItemRecommendation Engine",
      engineFactory = "io.prediction.engines.itemrec.ItemRecEngine",
      dataSourceJson = templates.itemrec.params.txt.datasourceJson(),
      preparatorJson = templates.itemrec.params.txt.preparatorJson(),
      algorithmsJson = templates.itemrec.params.txt.algorithmsJson(),
      servingJson = templates.itemrec.params.txt.servingJson())
  )

  // TODO: Check if engineFactory actuall exists.
  val idInstanceMap: Map[String, EngineInstanceTemplate] = instances
    .map { eit => (eit.engineId -> eit) }
    .toMap
}

