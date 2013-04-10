package io.prediction.commons.settings

/** OfflineEvalSplitterInfo object.
  *
  * @param id Unique identifier of a splitter.
  * @param name Splitter name.
  * @param description A long description of the splitter.
  * @param engineinfoids A list of EngineInfo IDs that this splitter can apply to.
  * @param commands A sequence of commands to run this metric.
  * @param paramdefaults Default parameters as key-value pairs. Usually used by substituting template variables in command templates.
  * @param paramnames Key value paris of (parameter -> display name).
  * @param paramdescription Key value paris of (parameter -> description).
  * @param paramorder The display order of parameters.
  */
case class OfflineEvalSplitterInfo(
  id: String,
  name: String,
  description: Option[String],
  engineinfoids: Seq[String],
  commands: Option[Seq[String]],
  paramdefaults: Map[String, Any],
  paramnames: Map[String, String],
  paramdescription: Map[String, String],
  paramorder: Seq[String]
)

/** Base trait for implementations that interact with metric info in the backend data store. */
trait OfflineEvalSplitterInfos {
  /** Inserts an metric info. */
  def insert(offlineEvalSplitterInfo: OfflineEvalSplitterInfo): Unit

  /** Get an metric info by its ID. */
  def get(id: String): Option[OfflineEvalSplitterInfo]

  /** Updates an metric info. */
  def update(offlineEvalSplitterInfo: OfflineEvalSplitterInfo): Unit

  /** Delete an metric info by its ID. */
  def delete(id: String): Unit
}
