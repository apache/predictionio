package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * OfflineEvalSplitter object.
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
  settings: Map[String, Any])

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
  def update(splitter: OfflineEvalSplitter, upsert: Boolean = false)

  /** Delete an offline evaluation splitter by its ID. */
  def delete(id: Int)

  implicit val formats = Serialization.formats(NoTypeHints) + new OfflineEvalSplitterSerializer

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll().toSeq).getBytes("UTF-8")

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[OfflineEvalSplitter]] = {
    try {
      val rdata = Serialization.read[Seq[OfflineEvalSplitter]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

/** json4s serializer for the OfflineEvalSplitter class. */
class OfflineEvalSplitterSerializer extends CustomSerializer[OfflineEvalSplitter](format => (
  {
    case x: JObject =>
      implicit val formats = Serialization.formats(NoTypeHints)
      OfflineEvalSplitter(
        id = (x \ "id").extract[Int],
        evalid = (x \ "evalid").extract[Int],
        name = (x \ "name").extract[String],
        infoid = (x \ "infoid").extract[String],
        settings = Common.sanitize((x \ "settings").asInstanceOf[JObject].values))
  },
  {
    case x: OfflineEvalSplitter =>
      implicit val formats = Serialization.formats(NoTypeHints)
      JObject(
        JField("id", Extraction.decompose(x.id)) ::
          JField("evalid", Extraction.decompose(x.evalid)) ::
          JField("name", Extraction.decompose(x.name)) ::
          JField("infoid", Extraction.decompose(x.infoid)) ::
          JField("settings", Extraction.decompose(x.settings)) :: Nil)
  })
)
