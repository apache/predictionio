package io.prediction.data.storage

import org.json4s._

import scala.collection.GenTraversableOnce

case class DataMapException(msg: String) extends Exception

case class DataMap (
  val fields: Map[String, JValue]
) extends Serializable {
  lazy implicit val formats = DefaultFormats +
    new DateTimeJson4sSupport.serializer

  def require(name: String) = {
    if (!fields.contains(name))
      throw new DataMapException(s"The field ${name} is required.")
  }

  def get[T: Manifest](name: String): T = {
    require(name)
    fields(name).extract[T]
  }
  // TODO: combine getOpt and get
  def getOpt[T: Manifest](name: String): Option[T] = {
    fields.get(name).map(_.extract[T])
  }

  def getOrElse[T: Manifest](name: String, default: T) = {
    fields.get(name).map(_.extract[T]).getOrElse(default)
  }

  def ++ (that: DataMap) = DataMap(this.fields ++ that.fields)

  def -- (that: GenTraversableOnce[String]) =
    DataMap(this.fields -- that)

  def isEmpty = fields.isEmpty

  def keySet = this.fields.keySet

  def toList(): List[(String, JValue)] = fields.toList

  def toJObject(): JObject = JObject(toList())

}

object DataMap {
  def apply(): DataMap = DataMap(Map[String, JValue]())

  def apply(jObj: JObject): DataMap = DataMap(jObj.obj.toMap)

}
