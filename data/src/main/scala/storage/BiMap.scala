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
class BiMap[K, V] private[prediction] (private val m: ImmutableBiMap[K, V])
  extends Serializable {
//class BiMap[K, V] (private val m: ImmutableBiMap[K, V]) extends Serializable {

  def this(x: Map[K, V]) = this(ImmutableBiMap.copyOf[K, V](x))

  def inverse: BiMap[V, K] = new BiMap(m.inverse())

  def get(k: K): Option[V] = Option(m.get(k))

  def getOrElse(k: K, default: => V): V = get(k).getOrElse(default)

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
}

object StrToLongBiMap {

  /** Create a String to Long BiMap from a set of String
    * @param keys a set of String
    * @return a String to Long BiMap
    */
  def apply(keys: Set[String]): BiMap[String, Long] = {
    val builder: ImmutableBiMap.Builder[String, Long] = ImmutableBiMap.builder()

    keys.zipWithIndex.foreach { case (k, v) =>
      builder.put(k, v)
    }
    new BiMap[String, Long](builder.build())
  }

  /** Create a String to Long BiMap from RDD[String]
    * @param keys RDD of String
    * @return a String to Long BiMap (local)
    */
  def apply(keys: RDD[String]): BiMap[String, Long] = {
    apply(keys.distinct.collect.toSet)
  }
}

object StrToIntBiMap {
  /** Create a String to Long BiMap from a set of String
    * @param keys a set of String
    * @return BiMap[String, Long] a String to Long BiMap
    */
  def apply(keys: Set[String]): BiMap[String, Int] = {
    val builder: ImmutableBiMap.Builder[String, Int] = ImmutableBiMap.builder()

    keys.zipWithIndex.foreach { case (k, v) =>
      builder.put(k, v)
    }
    new BiMap[String, Int](builder.build())
  }

  /** Create a String to Long BiMap from RDD[String]
    * @param keys RDD of String
    * @return a String to Long BiMap (local)
    */
  def apply(keys: RDD[String]): BiMap[String, Int] = {
    apply(keys.distinct.collect.toSet)
  }
}
