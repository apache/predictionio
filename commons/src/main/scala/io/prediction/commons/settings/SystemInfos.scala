package io.prediction.commons.settings

/** SystemInfo object.
  *
  * @param id Unique identifier of the info entry.
  * @param value Value of the info entry.
  * @param description A long description of the info entry.
  */
case class SystemInfo(
  id: String,
  value: String,
  description: Option[String]
)

/** Base trait for implementations that interact with system info in the backend data store. */
trait SystemInfos {
  /** Inserts a system info entry. */
  def insert(systemInfo: SystemInfo): Unit

  /** Get system info entry by its ID. */
  def get(id: String): Option[SystemInfo]

  /** Updates a system info entry. */
  def update(systemInfo: SystemInfo): Unit

  /** Delete a system info entry by its ID. */
  def delete(id: String): Unit
}
