package io.prediction.commons.settings

import io.prediction.commons.Common

import org.json4s._
import org.json4s.native.Serialization

/**
 * ParamGen Object
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
  params: Map[String, Any])

trait ParamGens extends Common {

  /** Insert a paramGen and return ID */
  def insert(paramGen: ParamGen): Int

  /** Get a paramGen by its ID */
  def get(id: Int): Option[ParamGen]

  /** Get all parameter generators. */
  def getAll(): Iterator[ParamGen]

  /** Get paramGen by offline tune ID */
  def getByTuneid(tuneid: Int): Iterator[ParamGen]

  /** Update paramGen */
  def update(paramGen: ParamGen, upsert: Boolean = false)

  /** Delete paramGen by its ID */
  def delete(id: Int)

  implicit val formats = Serialization.formats(NoTypeHints) + new ParamGenSerializer

  /** Backup all data as a byte array. */
  def backup(): Array[Byte] = Serialization.write(getAll().toSeq).getBytes("UTF-8")

  /** Restore data from a byte array backup created by the current or the immediate previous version of commons. */
  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[ParamGen]] = {
    try {
      val rdata = Serialization.read[Seq[ParamGen]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { update(_, true) }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

/** json4s serializer for the ParamGen class. */
class ParamGenSerializer extends CustomSerializer[ParamGen](format => (
  {
    case x: JObject =>
      implicit val formats = Serialization.formats(NoTypeHints)
      ParamGen(
        id = (x \ "id").extract[Int],
        infoid = (x \ "infoid").extract[String],
        tuneid = (x \ "tuneid").extract[Int],
        params = (x \ "params").asInstanceOf[JObject].values)
  },
  {
    case x: ParamGen =>
      implicit val formats = Serialization.formats(NoTypeHints)
      JObject(
        JField("id", Extraction.decompose(x.id)) ::
          JField("infoid", Extraction.decompose(x.infoid)) ::
          JField("tuneid", Extraction.decompose(x.tuneid)) ::
          JField("params", Extraction.decompose(x.params)) :: Nil)
  })
)
