package io.prediction.commons.modeldata


case class ItemSimScore(
  id: Int,
  iid: String,
  simiid: String,
  score: Double,
  itypes: String,
  algoid: Int,
  modelset: Boolean
)
/** Base trait */
trait ItemSimScores {
  /** Insert an ItemSimScore */
  def insert(itemsimscore : ItemSimScore): Int

  /** Get an ItemSimScore by id */
  def get(id : Int): Option[ItemSimScore]

  /** Delete an ItemSimScore by id */
  def delete(id : Int)
}
