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

import io.prediction.data.storage.Event
import io.prediction.data.storage.StorageClientConfig
import io.prediction.data.storage.hbase.HBPEvents
import io.prediction.data.storage.hbase.StorageClient

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

object HBPEventsTest {

  def main(args: Array[String]) {

    val fromAppId = args(0).toInt
    val toAppId = args(1).toInt

    val sparkConf = new SparkConf().setAppName("HBaseTest")
    val sc = new SparkContext(sparkConf)
    val eventClient = new HBPEvents(
      new StorageClient(new StorageClientConfig(Seq(), Seq(), true)).client,
      "predictionio_eventdata")
    val e1: RDD[Event] = eventClient.find(fromAppId)(sc).cache()

    eventClient.write(e1, toAppId)(sc)
    println("Finished writing.")

    // read back from toAppId
    val e2: RDD[Event] = eventClient.find(toAppId)(sc)

    val c1 = e1.count()
    val c2 = e2.count()

    // check data
    val a = e1.map { event => (event.eventId.getOrElse(""), event) }
    val b = e2.map { event => (event.eventId.getOrElse(""), event) }

    val notMatched1 = a.leftOuterJoin(b).map { case (k, (v, wOpt)) =>
      ((Some(v) == wOpt), (Some(v), wOpt))
    }.filter{ case (m, d) => (!m) }
    .cache()

    val notMatched2 = b.leftOuterJoin(a).map { case (k, (v, wOpt)) =>
      ((Some(v) == wOpt), (Some(v), wOpt))
    }.filter{ case (m, d) => (!m) }
    .cache()

    val notMatchedCount1 = notMatched1.count()
    if (notMatchedCount1 != 0) {
      notMatched1.take(2).foreach(println(_))
    }

    val notMatchedCount2 = notMatched2.count()
    if (notMatchedCount2 != 0) {
      notMatched2.take(2).foreach(println(_))
    }

    println(s"count1: ${c1}, count2: ${c2}")
    println(s"count match = ${c1 == c2}")
    println(s"data match1 = ${notMatchedCount1 == 0}")
    println(s"data match2 = ${notMatchedCount2 == 0}")
  }
}
