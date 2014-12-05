/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.data.storage

import org.json4s._

import scala.collection.GenTraversableOnce

/** Exception thrown by DataMap object.
  */
private[prediction] case class DataMapException(msg: String, cause: Exception)
  extends Exception(msg, cause) {

  def this(msg: String) = this(msg, null)
}

/** A DataMap stores properties of the event or entity. Internally it is a Map
  * whose keys are property names and values are corresponding JSON values
  * respectively. Use the get() method to retrieve the value of mandatory
  * property or use getOpt() to retrieve the value of the optional property.
  *
  * @param fields Map of property name to JValue
  */
case class DataMap (
  val fields: Map[String, JValue]
) extends Serializable {
  lazy implicit private val formats = DefaultFormats +
    new DateTimeJson4sSupport.serializer

  /** Check the existence of a required property name. Throw an exception if
    * it does not exist.
    *
    * @param name The property name
    */
  def require(name: String) = {
    if (!fields.contains(name))
      throw new DataMapException(s"The field ${name} is required.")
  }

  /** Check if this DataMap contains a specific property.
    *
    * @param name The property name
    * @return Return true if the property exists, else false.
    */
  def contains(name: String): Boolean = {
    fields.contains(name)
  }

  /** Get the value of a mandatory property. Exception is thrown if the property
    * does not exist.
    *
    * @tparam T The type of the property value
    * @param name The property name
    * @return Return the property value of type T
    */
  def get[T: Manifest](name: String): T = {
    require(name)
    fields(name) match {
      case JNull => throw new DataMapException(
        s"The required field ${name} cannot be null.")
      case x: JValue => x.extract[T]
    }
  }

  /** Get the value of an optional property. Return None if the property does
    * not exist.
    *
    * @tparam T The type of the property value
    * @param name The property name
    * @return Return the property value of type Option[T]
    */
  def getOpt[T: Manifest](name: String): Option[T] = {
    // either the field doesn't exist or its value is null
    fields.get(name).flatMap(_.extract[Option[T]])
  }

  /** Get the value of an optional property. Return default value if the
    * property does not exist.
    *
    * @tparam T The type of the property value
    * @param name The property name
    * @param default The default property value of type T
    * @return Return the property value of type T
    */
  def getOrElse[T: Manifest](name: String, default: T) = {
    getOpt[T](name).getOrElse(default)
  }

  /** Return a new DataMap with elements containing elements from the left hand
    * side operand followed by elements from the right hand side operand.
    *
    * @param that Right hand side DataMap
    * @return A new DataMap
    */
  def ++ (that: DataMap) = DataMap(this.fields ++ that.fields)

  /** Creates a new DataMap from this DataMap by removing all elements of
    * another collection.
    *
    * @param that A collection containing the removed property names
    * @return A new DataMap
    */
  def -- (that: GenTraversableOnce[String]) =
    DataMap(this.fields -- that)

  /** Tests whether the DataMap is empty.
    *
    * @return true if the DataMap is empty, false otherwise.
    */
  def isEmpty = fields.isEmpty

  /** Collects all property names of this DataMap in a set.
    *
    * @return a set containing all property names of this DataMap.
    */
  def keySet = this.fields.keySet

  /** Converts this DataMap to a List.
    *
    * @return a list of (property name, JSON value) tuples.
    */
  def toList(): List[(String, JValue)] = fields.toList

  /** Converts this DataMap to a JObject.
    *
    * @return the JObject initizalized by this DataMap.
    */
  def toJObject(): JObject = JObject(toList())

}

/** Companion object of the [[DataMap]] class. */
object DataMap {
  /** Create an empty DataMap
    * @return an empty DataMap
    */
  def apply(): DataMap = DataMap(Map[String, JValue]())

  /** Create an DataMap from a JObject
    * @param jObj JObject
    * @return a new DataMap initlized by a JObject
    */
  def apply(jObj: JObject): DataMap = {
    if (jObj == null) DataMap() else DataMap(jObj.obj.toMap)
  }
}
