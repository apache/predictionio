package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

/** ParamGenInfo object.
  *
  * @param id Unique identifier of a parameter generator.
  * @param name Generator name.
  * @param description A long description of the generator.
  * @param commands A sequence of commands to run this generator.
  * @param paramdefaults Default parameters as key-value pairs. Usually used by substituting template variables in command templates.
  * @param paramnames Key value paris of (parameter -> display name).
  * @param paramdescription Key value paris of (parameter -> description).
  * @param paramorder The display order of parameters.
  */
case class ParamGenInfo(
  id: String,
  name: String,
  description: Option[String],
  commands: Option[Seq[String]],
  paramdefaults: Map[String, Any],
  paramnames: Map[String, String],
  paramdescription: Map[String, String],
  paramorder: Seq[String]
)

/** Base trait for implementations that interact with parameter generator info in the backend data store. */
trait ParamGenInfos extends Common {
  /** Inserts a parameter generator info. */
  def insert(paramGenInfo: ParamGenInfo): Unit

  /** Get a parameter generator info by its ID. */
  def get(id: String): Option[ParamGenInfo]

  /** Get all parameter generator info. */
  def getAll(): Seq[ParamGenInfo]

  /** Updates a parameter generator info. */
  def update(paramGenInfo: ParamGenInfo, upsert: Boolean = false): Unit

  /** Delete a parameter generator info by its ID. */
  def delete(id: String): Unit

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = {
    val backup = getAll().map { b =>
      Map(
        "id" -> b.id,
        "name" -> b.name,
        "description" -> b.description,
        "commands" -> b.commands,
        "paramdefaults" -> b.paramdefaults,
        "paramnames" -> b.paramnames,
        "paramdescription" -> b.paramdescription,
        "paramorder" -> b.paramorder)
    }
    KryoInjection(backup)
  }

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[ParamGenInfo]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        ParamGenInfo(
          id = data("id").asInstanceOf[String],
          name = data("name").asInstanceOf[String],
          description = data("description").asInstanceOf[Option[String]],
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
