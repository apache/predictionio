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

class EntityMap[A](
  val idToIx: BiMap[String, Long],
  val idToData: Map[String, A]) {

  private val ixToId: BiMap[Long, String] = idToIx.inverse

  def apply(id: String): Long = idToIx(id)

  def apply(ix: Long): String = ixToId(ix)

  def data(id: String): A = idToData(id)

  def data(ix: Long): A = idToData(ixToId(ix))

  def contains(id: String): Boolean = idToIx.contains(id)

  def contains(ix: Long): Boolean = ixToId.contains(ix)

  def get(id: String): Option[Long] = idToIx.get(id)

  def get(ix: Long): Option[String] = ixToId.get(ix)

  def getOrElse(id: String, default: => Long): Long =
    idToIx.getOrElse(id, default)

  def getOrElse(ix: Long, default: => String): String =
    ixToId.getOrElse(ix, default)

  def getData(id: String): Option[A] = idToData.get(id)

  def getData(ix: Long): Option[A] = idToData.get(ixToId(ix))
  
}
