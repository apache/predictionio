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

import com.github.nscala_time.time.Imports._
import org.json4s._

/**
 * EvaluationInstance object.
 *
 * Stores meta information for each evaluation instance.
 *
 * @param id Instance ID.
 * @param status Status of this instance.
 * @param startTime Start time of this instance.
 * @param endTime End time of this instance.
 * @param engineFactory Engine factory class name of this instance.
 * @param evaluatorClass Evaluator class name of this instance.
 * @param batch Batch label of this instance.
 * @param env The environment in which this instance was created.
 * @param evaluatorResults Results of the evaluator.
 * @param evaluatorResultsHTML HTML results of the evaluator.
 * @param evaluatorResultsJSON JSON results of the evaluator.
 */
private[prediction] case class EvaluationInstance(
  id: String = "",
  status: String = "",
  startTime: DateTime = DateTime.now,
  endTime: DateTime = DateTime.now,
  engineFactory: String = "",
  evaluatorClass: String = "",
  batch: String = "",
  env: Map[String, String] = Map(),
  sparkConf: Map[String, String] = Map(),
  evaluatorResults: String = "",
  evaluatorResultsHTML: String = "",
  evaluatorResultsJSON: String = "")

/**
 * Base trait for implementations that interact with EvaluationInstances in the
 * backend data store.
 */
private[prediction] trait EvaluationInstances {
  /** Insert a new EvaluationInstance. */
  def insert(i: EvaluationInstance): String

  /** Get an EvaluationInstance by ID. */
  def get(id: String): Option[EvaluationInstance]

  /** Get all EvaluationInstances. */
  def getAll: Seq[EvaluationInstance]

  /** Get instances that are produced by evaluation and have run to completion,
    * reverse sorted by the start time.
    */
  def getCompleted: Seq[EvaluationInstance]

  /** Update an EvaluationInstance. */
  def update(i: EvaluationInstance): Unit

  /** Delete an EvaluationInstance. */
  def delete(id: String): Unit
}

private[prediction]
class EvaluationInstanceSerializer extends CustomSerializer[EvaluationInstance](
  format => ({
    case JObject(fields) =>
      implicit val formats = DefaultFormats
      fields.foldLeft(EvaluationInstance()) { case (i, field) =>
        field match {
          case JField("id", JString(id)) => i.copy(id = id)
          case JField("status", JString(status)) => i.copy(status = status)
          case JField("startTime", JString(startTime)) =>
            i.copy(startTime = Utils.stringToDateTime(startTime))
          case JField("endTime", JString(endTime)) =>
            i.copy(endTime = Utils.stringToDateTime(endTime))
          case JField("engineFactory", JString(engineFactory)) =>
            i.copy(engineFactory = engineFactory)
          case JField("metricsClass", JString(evaluatorClass)) =>
            i.copy(evaluatorClass = evaluatorClass)
          case JField("batch", JString(batch)) => i.copy(batch = batch)
          case JField("env", env) =>
            i.copy(env = Extraction.extract[Map[String, String]](env))
          case JField("sparkConf", sparkConf) =>
            i.copy(sparkConf = Extraction.extract[Map[String, String]](sparkConf))
          case JField("multipleMetricsResults",
          JString(evaluatorResults)) =>
            i.copy(evaluatorResults = evaluatorResults)
          case JField("multipleMetricsResultsHTML",
          JString(evaluatorResultsHTML)) =>
            i.copy(evaluatorResultsHTML = evaluatorResultsHTML)
          case JField("multipleMetricsResultsJSON",
          JString(evaluatorResultsJSON)) =>
            i.copy(evaluatorResultsJSON = evaluatorResultsJSON)
          case _ => i
        }
      }
  }, {
    case i: EvaluationInstance =>
      JObject(
        JField("id", JString(i.id)) ::
          JField("status", JString(i.status)) ::
          JField("startTime", JString(i.startTime.toString)) ::
          JField("endTime", JString(i.endTime.toString)) ::
          JField("engineFactory", JString(i.engineFactory)) ::
          JField("metricsClass", JString(i.evaluatorClass)) ::
          JField("batch", JString(i.batch)) ::
          JField("env", Extraction.decompose(i.env)(DefaultFormats)) ::
          JField("sparkConf", Extraction.decompose(i.sparkConf)(DefaultFormats)) ::
          JField("multipleMetricsResults",
            JString(i.evaluatorResults)) ::
          JField("multipleMetricsResultsHTML",
            JString(i.evaluatorResultsHTML)) ::
          JField("multipleMetricsResultsJSON",
            JString(i.evaluatorResultsJSON)) ::
          Nil
      )
  }
  )
)
