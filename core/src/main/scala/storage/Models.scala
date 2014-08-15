package io.prediction.storage

import com.google.common.io.BaseEncoding
import org.json4s._
import org.json4s.native.Serialization

/**
 * Model object.
 *
 * Stores model for each engine instance.
 *
 * @param id ID of the model, which should be the same as engine instance ID.
 * @param models Trained models of all algorithms.
 */
case class Model(
  id: String,
  models: Array[Byte])

/**
 * Base trait for implementations that interact with Models in the backend data
 * store.
 */
trait Models {
  /** Insert a new Model. */
  def insert(i: Model): Unit

  /** Get a Model by ID. */
  def get(id: String): Option[Model]

  /** Delete a Model. */
  def delete(id: String): Unit
}

class ModelSerializer extends CustomSerializer[Model](
  format => ({
    case JObject(fields) =>
      implicit val formats = DefaultFormats
      val seed = Model(
          id = "",
          models = Array[Byte]())
      fields.foldLeft(seed) { case (i, field) =>
        field match {
          case JField("id", JString(id)) => i.copy(id = id)
          case JField("models", JString(models)) =>
            i.copy(models = BaseEncoding.base64.decode(models))
          case _ => i
        }
      }
  },
  {
    case i: Model =>
      JObject(
        JField("id", JString(i.id)) ::
        JField("models", JString(BaseEncoding.base64.encode(i.models))) ::
        Nil)
  }
))
