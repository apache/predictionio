/** Copyright 2015 TappingStone, Inc.
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
 * Channel object.
 *
 * Stores mapping of channel IDs, names and app ID
 *
 * @param id ID of the channel
 * @param name Name of the channel (must be unique within the same app).
 * @param appid ID of the app which this channel belongs to
 */
private[prediction] case class Channel(
  id: Int,
  name: String, // must be unique within the same app
  appid: Int
) {
  require(Channel.isValidName(name),
    "Invalid channel name: ${name}. ${Channel.nameConstraint}")
}

private[prediction] object Channel {
  def isValidName(s: String): Boolean = {
    // note: update channelNameConstraint if this rule is changed
    s.matches("^[a-zA-Z0-9-]{1,16}$")
  }

  // for display error message consistently
  val nameConstraint: String =
    "Only alphanumeric and - characters are allowed and max length is 16."
}

/**
 * Base trait for implementations that interact with Channels in the backend
 * data store.
 */
private[prediction] trait Channels {

  /** Insert a new Channel. Returns a generated channel ID if original ID is 0. */
  def insert(channel: Channel): Option[Int]

  /** Get a Channel by channel ID. */
  def get(id: Int): Option[Channel]

  /** Get all Channel by app ID. */
  def getByAppid(appid: Int): Seq[Channel]

  /** Delete a Channel */
  def delete(id: Int): Boolean

}
