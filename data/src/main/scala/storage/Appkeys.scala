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

/**
 * Appkey object.
 *
 * Stores mapping of app keys, IDs, and lists of allowed event names.
 *
 * @param appkey Key of the app.
 * @param appid ID of the app.
 * @param events List of allowed events for this particular app key.
 */
case class Appkey(
  appkey: String,
  appid: Int,
  events: Seq[String])

/**
 * Base trait for implementations that interact with Appkeys in the backend data
 * store.
 */
trait Appkeys {
  /** Insert a new Appkey. Returns a generated app key. */
  def insert(k: Appkey): Option[String]

  /** Get an Appkey by app key. */
  def get(k: String): Option[Appkey]

  /** Get all Appkeys. */
  def getAll(): Seq[Appkey]

  /** Get all Appkeys for a particular app ID. */
  def getByAppid(appid: Int): Seq[Appkey]

  /** Update an Appkey. */
  def update(k: Appkey): Boolean

  /** Delete an Appkey. */
  def delete(k: String): Boolean
}
