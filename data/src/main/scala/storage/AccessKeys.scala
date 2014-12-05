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
 * AccessKey object.
 *
 * Stores mapping of access keys, IDs, and lists of allowed event names.
 *
 * @param key Key.
 * @param appid ID of the app.
 * @param events List of allowed events for this particular app key.
 */
private[prediction] case class AccessKey(
  key: String,
  appid: Int,
  events: Seq[String])

/**
 * Base trait for implementations that interact with AcessKeys in the backend
 * data store.
 */
private[prediction] trait AccessKeys {
  /** Insert a new AccessKey. Returns a generated access key. */
  def insert(k: AccessKey): Option[String]

  /** Get an AccessKey by key. */
  def get(k: String): Option[AccessKey]

  /** Get all AccessKeys. */
  def getAll(): Seq[AccessKey]

  /** Get all AccessKeys for a particular app ID. */
  def getByAppid(appid: Int): Seq[AccessKey]

  /** Update an AccessKey. */
  def update(k: AccessKey): Boolean

  /** Delete an AccessKey. */
  def delete(k: String): Boolean
}
