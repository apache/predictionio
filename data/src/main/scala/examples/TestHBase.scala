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

package io.prediction.data.storage.examples

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.HConnectionManager
import org.apache.hadoop.hbase.client.HConnection
import org.apache.hadoop.hbase.client.HBaseAdmin

import io.prediction.data.storage.Storage
import io.prediction.data.storage.Event

import scala.concurrent.ExecutionContext.Implicits.global

// sbt/sbt "data/run-main io.prediction.data.storage.examples.TestHBase"
object TestHBase {

  def main(arg: Array[String]) {

    /*
    val conf = HBaseConfiguration.create()
    conf.set("hbase.client.retries.number", "1")
    conf.set("zookeeper.recovery.retry", "1")

    HBaseAdmin.checkHBaseAvailable(conf)
    val a = new HBaseAdmin(conf)
    a.listTables()
    */
    println("  Verifying Event Data Backend")
    val eventsDb = Storage.getLEvents()
    eventsDb.init(0)
    eventsDb.insert(Event(
      event="test",
      entityType="test",
      entityId="test"), 0)
    eventsDb.remove(0)
    eventsDb.close()

  }
}
