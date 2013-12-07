package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

/**
 * OfflineEvalSplitterInfo object.
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
  paramorder: Seq[String])

/** Base trait for implementations that interact with metric info in the backend data store. */
trait OfflineEvalSplitterInfos extends Common {
  /** Inserts a splitter info. */
  def insert(offlineEvalSplitterInfo: OfflineEvalSplitterInfo): Unit

  /** Get a splitter info by its ID. */
  def get(id: String): Option[OfflineEvalSplitterInfo]

  /** Get all splitter info. */
  def getAll(): Seq[OfflineEvalSplitterInfo]

  /** Updates a splitter info. */
  def update(offlineEvalSplitterInfo: OfflineEvalSplitterInfo, upsert: Boolean = false): Unit

  /** Delete a splitter info by its ID. */
  def delete(id: String): Unit

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = {
    val backup = getAll().map { b =>
      Map(
        "id" -> b.id,
        "name" -> b.name,
        "description" -> b.description,
        "engineinfoids" -> b.engineinfoids,
        "commands" -> b.commands,
        "paramdefaults" -> b.paramdefaults,
        "paramnames" -> b.paramnames,
        "paramdescription" -> b.paramdescription,
        "paramorder" -> b.paramorder)
    }
    KryoInjection(backup)
  }

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalSplitterInfo]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        OfflineEvalSplitterInfo(
          id = data("id").asInstanceOf[String],
          name = data("name").asInstanceOf[String],
          description = data("description").asInstanceOf[Option[String]],
          engineinfoids = data("engineinfoids").asInstanceOf[Seq[String]],
          commands = data("commands").asInstanceOf[Option[Seq[String]]],
          paramdefaults = data("paramdefaults").asInstanceOf[Map[String, Any]],
          paramnames = data("paramnames").asInstanceOf[Map[String, String]],
          paramdescription = data("paramdescription").asInstanceOf[Map[String, String]],
          paramorder = data("paramorder").asInstanceOf[Seq[String]])
      }

      if (inplace) rdata foreach { update(_, true) }

      rdata
    }
  }
}
