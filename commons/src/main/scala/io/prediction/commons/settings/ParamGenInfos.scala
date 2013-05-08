package io.prediction.commons.settings

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
trait ParamGenInfos {
  /** Inserts a parameter generator info. */
  def insert(paramGenInfo: ParamGenInfo): Unit

  /** Get a parameter generator by its ID. */
  def get(id: String): Option[ParamGenInfo]

  /** Updates a parameter generator info. */
  def update(paramGenInfo: ParamGenInfo): Unit

  /** Delete a parameter generator info by its ID. */
  def delete(id: String): Unit
}
