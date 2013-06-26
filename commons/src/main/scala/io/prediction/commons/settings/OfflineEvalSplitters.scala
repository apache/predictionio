package io.prediction.commons.settings

import io.prediction.commons.Common

import com.twitter.chill.KryoInjection

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
trait OfflineEvalSplitters extends Common {
  /** Inserts an offline evaluation splitter. */
  def insert(splitter: OfflineEvalSplitter): Int

  /** Get an offline evaluation splitter by its ID. */
  def get(id: Int): Option[OfflineEvalSplitter]

  /** Get all offline evaluation splitters. */
  def getAll(): Iterator[OfflineEvalSplitter]

  /** Get offline evluation splitters by Offline Eval ID */
  def getByEvalid(evalid: Int): Iterator[OfflineEvalSplitter]

  /** Update an offline evaluation splitter. */
  def update(splitter: OfflineEvalSplitter)

  /** Delete an offline evaluation splitter by its ID. */
  def delete(id: Int)

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = {
    val backup = getAll().toSeq.map { b =>
      Map(
        "id" -> b.id,
        "evalid" -> b.evalid,
        "name" -> b.name,
        "infoid" -> b.infoid,
        "settings" -> b.settings)
    }
    KryoInjection(backup)
  }

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], upgrade: Boolean = false): Option[Seq[OfflineEvalSplitter]] = {
    KryoInjection.invert(bytes) map { r =>
      r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        OfflineEvalSplitter(
          id = data("id").asInstanceOf[Int],
          evalid = data("evalid").asInstanceOf[Int],
          name = data("name").asInstanceOf[String],
          infoid = data("infoid").asInstanceOf[String],
          settings = data("settings").asInstanceOf[Map[String, Any]])
      }
    }
  }
}
