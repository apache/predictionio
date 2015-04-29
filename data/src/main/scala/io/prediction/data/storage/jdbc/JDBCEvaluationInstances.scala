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

package io.prediction.data.storage.jdbc

import grizzled.slf4j.Logging
import io.prediction.data.storage.EvaluationInstance
import io.prediction.data.storage.EvaluationInstances
import io.prediction.data.storage.StorageClientConfig
import scalikejdbc._

class JDBCEvaluationInstances(client: String, config: StorageClientConfig, prefix: String)
  extends EvaluationInstances with Logging {
  val tableName = JDBCUtils.prefixTableName(prefix, "evaluationinstances")
  DB autoCommit { implicit session =>
    try {
      sql"""
      create table $tableName (
        id text not null primary key,
        status text not null,
        startTime timestamp not null,
        endTime timestamp not null,
        evaluationClass text not null,
        engineParamsGeneratorClass text not null,
        batch text not null,
        env text not null,
        sparkConf text not null,
        evaluatorResults text not null,
        evaluatorResultsHTML text not null,
        evaluatorResultsJSON text)""".execute().apply()
    } catch {
      case e: Exception => debug(e.getMessage, e)
    }
  }

  def insert(i: EvaluationInstance): String = DB localTx { implicit session =>
    try {
      val id = java.util.UUID.randomUUID().toString
      sql"""
      INSERT INTO $tableName VALUES(
        $id,
        ${i.status},
        ${i.startTime},
        ${i.endTime},
        ${i.evaluationClass},
        ${i.engineParamsGeneratorClass},
        ${i.batch},
        ${JDBCUtils.mapToString(i.env)},
        ${JDBCUtils.mapToString(i.sparkConf)},
        ${i.evaluatorResults},
        ${i.evaluatorResultsHTML},
        ${i.evaluatorResultsJSON})""".update().apply()
      id
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        ""
    }
  }

  def get(id: String): Option[EvaluationInstance] = DB localTx { implicit session =>
    try {
      sql"""
      SELECT
        id,
        status,
        startTime,
        endTime,
        evaluationClass,
        engineParamsGeneratorClass,
        batch,
        env,
        sparkConf,
        evaluatorResults,
        evaluatorResultsHTML,
        evaluatorResultsJSON
      FROM $tableName WHERE id = $id
      """.map(resultToEvaluationInstance).single().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        None
    }
  }

  def getAll(): Seq[EvaluationInstance] = DB localTx { implicit session =>
    try {
      sql"""
      SELECT
        id,
        status,
        startTime,
        endTime,
        evaluationClass,
        engineParamsGeneratorClass,
        batch,
        env,
        sparkConf,
        evaluatorResults,
        evaluatorResultsHTML,
        evaluatorResultsJSON
      FROM $tableName
      """.map(resultToEvaluationInstance).list().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        Seq()
    }
  }

  def getCompleted(): Seq[EvaluationInstance] = DB localTx { implicit s =>
    try {
      sql"""
      SELECT
        id,
        status,
        startTime,
        endTime,
        evaluationClass,
        engineParamsGeneratorClass,
        batch,
        env,
        sparkConf,
        evaluatorResults,
        evaluatorResultsHTML,
        evaluatorResultsJSON
      FROM $tableName
      WHERE
        status = 'EVALCOMPLETED'
      ORDER BY starttime DESC
      """.map(resultToEvaluationInstance).list().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        Seq()
    }
  }

  def update(i: EvaluationInstance): Unit = DB localTx { implicit session =>
    try {
      sql"""
      update $tableName set
        status = ${i.status},
        startTime = ${i.startTime},
        endTime = ${i.endTime},
        evaluationClass = ${i.evaluationClass},
        engineParamsGeneratorClass = ${i.engineParamsGeneratorClass},
        batch = ${i.batch},
        env = ${JDBCUtils.mapToString(i.env)},
        sparkConf = ${JDBCUtils.mapToString(i.sparkConf)},
        evaluatorResults = ${i.evaluatorResults},
        evaluatorResultsHTML = ${i.evaluatorResultsHTML},
        evaluatorResultsJSON = ${i.evaluatorResultsJSON}
      where id = ${i.id}""".update().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        ""
    }
  }

  /** Delete a EngineInstance. */
  def delete(id: String): Unit = DB localTx { implicit session =>
    try {
      sql"DELETE FROM $tableName WHERE id = $id".update().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
    }
  }

  def resultToEvaluationInstance(rs: WrappedResultSet): EvaluationInstance = {
    EvaluationInstance(
      id = rs.string("id"),
      status = rs.string("status"),
      startTime = rs.jodaDateTime("startTime"),
      endTime = rs.jodaDateTime("endTime"),
      evaluationClass = rs.string("evaluationClass"),
      engineParamsGeneratorClass = rs.string("engineParamsGeneratorClass"),
      batch = rs.string("batch"),
      env = JDBCUtils.stringToMap(rs.string("env")),
      sparkConf = JDBCUtils.stringToMap(rs.string("sparkConf")),
      evaluatorResults = rs.string("evaluatorResults"),
      evaluatorResultsHTML = rs.string("evaluatorResultsHTML"),
      evaluatorResultsJSON = rs.string("evaluatorResultsJSON"))
  }
}
