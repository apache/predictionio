package io.prediction.commons.settings.mongodb

import io.prediction.commons.settings.Metadata

import com.mongodb.casbah.Imports._
import org.json4s._
import org.json4s.native.Serialization

/** MongoDB implementation of AlgoInfos. */
class MongoMetadata(db: MongoDB) extends Metadata {
  private val seqColl = db("seq")

  implicit val formats = Serialization.formats(NoTypeHints)

  def backup(): Array[Byte] = {
    val backup = seqColl.find().toSeq.map { b =>
      try {
        MongoMetadataEntry(id = b.as[String]("_id"), next = b.as[Double]("next"))
      } catch {
        case e: ClassCastException =>
          MongoMetadataEntry(id = b.as[String]("_id"), next = b.as[Int]("next").toDouble)
      }
    }
    Serialization.write(backup).getBytes("UTF-8")
  }

  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[MongoMetadataEntry]] = {
    try {
      val rdata = Serialization.read[Seq[MongoMetadataEntry]](new String(bytes, "UTF-8"))
      if (inplace) rdata foreach { data =>
        val idObj = MongoDBObject("_id" -> data.id)
        seqColl.update(
          idObj,
          idObj ++ MongoDBObject("next" -> data.next),
          true)
      }
      Some(rdata)
    } catch {
      case e: MappingException => None
    }
  }
}

case class MongoMetadataEntry(id: String, next: Double)
