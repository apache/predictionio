package io.prediction.commons.settings

import com.github.nscala_time.time.Imports._


/** OfflineTune Object
 *
 * @param id Id
 * @param engineid The Engine ID
 * @param loops Number of offline tune loops
 * @param createtime The Creation time of the offline tune
 * @param starttime The Starting time of the offline tune
 * @param endtime The End time of the the offline tune
 */
case class OfflineTune(
  id: Int,
  engineid: Int,
  loops: Int,
  createtime: Option[DateTime],
  starttime: Option[DateTime],
  endtime: Option[DateTime]
)

trait OfflineTunes {

  /** Insert an OfflineTune and return its ID. */
  def insert(offlineTune: OfflineTune): Int

  /** Get OfflineTune by its ID. */
  def get(id: Int): Option[OfflineTune]

  /** Get OfflineTune's by Engine ID. */
  def getByEngineid(engineid: Int): Iterator[OfflineTune]

  /** Update OfflineTune (create new one if the it doesn't exist). */
  def update(offlineTune: OfflineTune)

  /** Delete OfflineTune by its ID. */
  def delete(id: Int)

}