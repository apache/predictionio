package io.prediction.commons.modeldata


case class ItemRecScore(
  uid: String,
  iid: String,
  score: Double,
  itypes: List[String],
  appid: Int,
  algoid: Int,
  modelset: Boolean
)

/** Base trait */
trait ItemRecScores {
  /** Insert an ItemSimScore */
  def insert(itemrecscore: ItemRecScore): Unit

  /** Get an ItemSimScore by id */
  def get(appid: Int, uid: String, n: Int, modelset: Boolean): List[ItemRecScore]

  def getAll(appid: Int, uid:String, modelset: Boolean) : Iterator[ItemRecScore]
}
