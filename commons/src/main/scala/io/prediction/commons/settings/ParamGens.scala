package io.prediction.commons.settings

/** ParamGen Object
 *
 * @param id ID
 * @param infoid param gen info id
 * @param tuneid ID of the OfflineTune
 * @param params param gen parameters as key-value pairs
 */
case class ParamGen(
  id: Int,
  infoid: String,
  tuneid: Int,
  params: Map[String, Any]
)

trait ParamGens {

  /** Insert a paramGen and return ID */
  def insert(paramGen: ParamGen): Int

  /** Get a paramGen by its ID */
  def get(id: Int): Option[ParamGen]

  /** Update paramGen */
  def update(paramGen: ParamGen)

  /** Delete paramGen by its ID */
  def delete(id: Int)
}
