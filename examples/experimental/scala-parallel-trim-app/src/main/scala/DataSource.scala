/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.predictionio.examples.experimental.trimapp

import org.apache.predictionio.controller.PDataSource
import org.apache.predictionio.controller.EmptyEvaluationInfo
import org.apache.predictionio.controller.EmptyActualResult
import org.apache.predictionio.controller.Params
import org.apache.predictionio.data.storage.Event
import org.apache.predictionio.data.storage.Storage

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import com.github.nscala_time.time.Imports._

import grizzled.slf4j.Logger

case class DataSourceParams(
  srcAppId: Int,
  dstAppId: Int,
  startTime: Option[DateTime],
  untilTime: Option[DateTime]
) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData,
      EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    val eventsDb = Storage.getPEvents()
    logger.info(s"TrimApp: $dsp")


    logger.info(s"Read events from appId ${dsp.srcAppId}")
    val srcEvents: RDD[Event] = eventsDb.find(
      appId = dsp.srcAppId,
      startTime = dsp.startTime,
      untilTime = dsp.untilTime
    )(sc)

    val dstEvents: Array[Event] = eventsDb.find(appId = dsp.dstAppId)(sc).take(1)

    if (dstEvents.size > 0) {
      throw new Exception(s"DstApp ${dsp.dstAppId} is not empty. Quitting.")
    }

    logger.info(s"Write events to appId ${dsp.dstAppId}")
    eventsDb.write(srcEvents, dsp.dstAppId)(sc)
    
    logger.info(s"Finish writing events to appId ${dsp.dstAppId}")

    new TrainingData()
  }
}

class TrainingData(
) extends Serializable {
  override def toString = ""
}
