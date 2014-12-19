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

import com.google.common.collect.ImmutableBiMap
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._

/** Immutable Bi-directional Map
  *
  */
class BiMap[K, V] private[prediction] (
  private val m: ImmutableBiMap[K, V],
  private val i: Option[BiMap[V, K]] = None
  ) extends Serializable {

  val inverse: BiMap[V, K] = i.getOrElse(new BiMap(m.inverse, Some(this)))

  def get(k: K): Option[V] = Option(m.get(k))

  def getOrElse(k: K, default: => V): V = get(k).getOrElse(default)

  def contains(k: K): Boolean = m.containsKey(k)

  def apply(k: K): V = {
    if (m.containsKey(k))
      m.get(k)
    else
      throw new java.util.NoSuchElementException(s"key not found: ${k}")
  }

  /** Converts to a map.
    * @return a map of type immutable.Map[K, V]
    */
  def toMap: Map[K, V] = m.toMap

  /** Converts to a sequence.
    * @return a sequence containing all elements of this map
    */
  def toSeq: Seq[(K, V)] = m.toSeq

  def size: Int = m.size

  def take(n: Int) = BiMap(m.toMap.take(n))

  override def toString = m.toString
}

object BiMap {

  def apply[K, V](x: Map[K, V]): BiMap[K, V] =
    new BiMap(ImmutableBiMap.copyOf[K, V](x))

  /** Create a BiMap[String, Long] from a set of String. The Long index starts
    * from 0.
    * @param keys a set of String
    * @return a String to Long BiMap
    */
  def stringLong(keys: Set[String]): BiMap[String, Long] = {
    val builder: ImmutableBiMap.Builder[String, Long] = ImmutableBiMap.builder()

    keys.zipWithIndex.foreach { case (k, v) =>
      builder.put(k, v)
    }
    new BiMap(builder.build())
  }

  /** Create a BiMap[String, Long] from an array of String.
    * NOTE: the the array cannot have duplicated element.
    * The Long index starts from 0.
    * @param keys a set of String
    * @return a String to Long BiMap
    */
  def stringLong(keys: Array[String]): BiMap[String, Long] = {
    val builder: ImmutableBiMap.Builder[String, Long] = ImmutableBiMap.builder()

    keys.zipWithIndex.foreach { case (k, v) =>
      builder.put(k, v)
    }
    new BiMap(builder.build())
  }

  /** Create a BiMap[String, Long] from RDD[String]. The Long index starts
    * from 0.
    * @param keys RDD of String
    * @return a String to Long BiMap
    */
  def stringLong(keys: RDD[String]): BiMap[String, Long] = {
    stringLong(keys.distinct.collect)
  }

  /** Create a BiMap[String, Int] from a set of String. The Int index starts
    * from 0.
    * @param keys a set of String
    * @return a String to Int BiMap
    */
  def stringInt(keys: Set[String]): BiMap[String, Int] = {
    val builder: ImmutableBiMap.Builder[String, Int] = ImmutableBiMap.builder()

    keys.zipWithIndex.foreach { case (k, v) =>
      builder.put(k, v)
    }
    new BiMap(builder.build())
  }

  /** Create a BiMap[String, Int] from an array of String.
    * NOTE: the the array cannot have duplicated element.
    * The Int index starts from 0.
    * @param keys a set of String
    * @return a String to Int BiMap
    */
  def stringInt(keys: Array[String]): BiMap[String, Int] = {
    val builder: ImmutableBiMap.Builder[String, Int] = ImmutableBiMap.builder()

    keys.zipWithIndex.foreach { case (k, v) =>
      builder.put(k, v)
    }
    new BiMap(builder.build())
  }

  /** Create a BiMap[String, Int] from RDD[String]. The Int index starts
    * from 0.
    * @param keys RDD of String
    * @return a String to Int BiMap
    */
  def stringInt(keys: RDD[String]): BiMap[String, Int] = {
    stringInt(keys.distinct.collect)
  }

}
