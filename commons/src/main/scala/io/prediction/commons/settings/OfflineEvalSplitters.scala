package io.prediction.commons.settings

/** OfflineEvalSplitter object.
  *
  * @param id ID.
  * @param evalid Eval ID that owns this split.
  * @param name Split name.
  * @param infoid OfflineEvalSplitInfo ID.
  * @param settings Split settings as key-value pairs.
  */
case class OfflineEvalSplitter(
  id: Int,
  evalid: Int,
  name: String,
  infoid: String,
  settings: Map[String, Any]
)

/** Base trait for implementations that interact with engines in the backend data store. */
trait OfflineEvalSplitters {
  /** Inserts an offline evaluation splitter. */
  def insert(splitter: OfflineEvalSplitter): Int

  /** Get an offline evaluation splitter by its ID. */
  def get(id: Int): Option[OfflineEvalSplitter]

  /** Get offline evluation splitters by Offline Eval ID */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalSplitter]

  /** Update an offline evaluation splitter. */
  def update(splitter: OfflineEvalSplitter)

  /** Delete an offline evaluation splitter by its ID. */
  def delete(id: Int)
}
