package io.prediction.commons.settings.mongodb

import io.prediction.commons.settings.Metadata

import com.mongodb.casbah.Imports._
import com.twitter.chill.KryoInjection

/** MongoDB implementation of AlgoInfos. */
class MongoMetadata(db: MongoDB) extends Metadata {
  private val seqColl = db("seq")

  def backup(): Array[Byte] = {
    val backup = seqColl.find().toSeq.map { b =>
      Map(
        "id"   -> b.as[String]("_id"),
        "next" -> b.as[Int]("next"))
    }
    KryoInjection(backup)
  }

  def restore(bytes: Array[Byte], inplace: Boolean = false, upgrade: Boolean = false): Option[Seq[Map[String, Any]]] = {
    KryoInjection.invert(bytes) map { r =>
      val rdata = r.asInstanceOf[Seq[Map[String, Any]]] map { data =>
        Map(
          "id"   -> data("id").asInstanceOf[String],
          "next" -> data("next").asInstanceOf[Int])
      }

      if (inplace) rdata foreach { data =>
        val idObj = MongoDBObject("_id" -> data("id"))
        seqColl.update(
          idObj,
          idObj ++ MongoDBObject("next" -> data("next")),
          true)
      }

      rdata
    }
  }
}
