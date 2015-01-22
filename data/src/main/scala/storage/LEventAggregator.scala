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

private[prediction] object LEventAggregator {

  def aggregateProperties(events: Iterator[Event]): Map[String, DataMap] = {
    events.toList
      .groupBy(_.entityId)
      .mapValues(_.sortBy(_.eventTime.getMillis)
        .foldLeft[Option[DataMap]](None)(dataMapAggregator))
      .filter{ case (k, v) => v.isDefined }
      .mapValues(_.get)
  }

  // aggregate Properties for single entity
  def aggregatePropertiesSingle(events: Iterator[Event]): Option[DataMap] = {
    events.toList
      .sortBy(_.eventTime.getMillis)
      .foldLeft[Option[DataMap]](None)(dataMapAggregator)
  }

  val eventNames = List("$set", "$unset", "$delete")

  private
  def dataMapAggregator: ((Option[DataMap], Event) => Option[DataMap]) = {
    (p, e) => {
      e.event match {
        case "$set" => {
          if (p == None)
            Some(e.properties)
          else
            p.map(_ ++ e.properties)
        }
        case "$unset" => {
          if (p == None)
            None
          else
            p.map(_ -- e.properties.keySet)
        }
        case "$delete" => None
        case _ => p // do nothing for others
      }
    }
  }
}
