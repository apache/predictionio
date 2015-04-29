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
import io.prediction.data.storage.EngineInstance
import io.prediction.data.storage.EngineInstances
import io.prediction.data.storage.StorageClientConfig
import scalikejdbc._

class JDBCEngineInstances(client: String, config: StorageClientConfig, database: String)
  extends EngineInstances with Logging {
  DB autoCommit { implicit session =>
    try {
      sql"""
      create table engineinstances (
        id text not null primary key,
        status text not null,
        starttime timestamp not null,
        endtime timestamp not null,
        engineid text not null,
        engineversion text not null,
        enginevariant text not null,
        enginefactory text not null,
        evaluatorclass text not null,
        batch text not null,
        env text not null,
        sparkconf text not null,
        datasourceparams text not null,
        preparatorparams text not null,
        algorithmsparams text not null,
        servingparams text not null,
        evaluatorparams text not null,
        evaluatorresults text not null,
        evaluatorresultshtml text not null,
        evaluatorresultsjson text)""".execute().apply()
    } catch {
      case e: Exception => debug(e.getMessage, e)
    }
  }

  def insert(i: EngineInstance): String = DB localTx { implicit session =>
    try {
      val id = java.util.UUID.randomUUID().toString
      sql"""
      INSERT INTO engineinstances VALUES(
        $id,
        ${i.status},
        ${i.startTime},
        ${i.endTime},
        ${i.engineId},
        ${i.engineVersion},
        ${i.engineVariant},
        ${i.engineFactory},
        ${i.evaluatorClass},
        ${i.batch},
        ${JDBCUtils.mapToString(i.env)},
        ${JDBCUtils.mapToString(i.sparkConf)},
        ${i.dataSourceParams},
        ${i.preparatorParams},
        ${i.algorithmsParams},
        ${i.servingParams},
        ${i.evaluatorParams},
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

  def get(id: String): Option[EngineInstance] = DB localTx { implicit session =>
    try {
      sql"""
      SELECT
        id,
        status,
        starttime,
        endtime,
        engineid,
        engineversion,
        enginevariant,
        enginefactory,
        evaluatorclass,
        batch,
        env,
        sparkconf,
        datasourceparams,
        preparatorparams,
        algorithmsparams,
        servingparams,
        evaluatorparams,
        evaluatorresults,
        evaluatorresultshtml,
        evaluatorresultsjson
      FROM engineinstances WHERE id = $id""".map(resultToEngineInstance).
        single().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        None
    }
  }

  def getAll(): Seq[EngineInstance] = DB localTx { implicit session =>
    try {
      sql"""
      SELECT
        id,
        status,
        starttime,
        endtime,
        engineid,
        engineversion,
        enginevariant,
        enginefactory,
        evaluatorclass,
        batch,
        env,
        sparkconf,
        datasourceparams,
        preparatorparams,
        algorithmsparams,
        servingparams,
        evaluatorparams,
        evaluatorresults,
        evaluatorresultshtml,
        evaluatorresultsjson
      FROM engineinstances""".map(resultToEngineInstance).list().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        Seq()
    }
  }

  def getLatestCompleted(
    engineId: String,
    engineVersion: String,
    engineVariant: String): Option[EngineInstance] =
    getCompleted(engineId, engineVersion, engineVariant).headOption

  /** Get all instances that has trained to completion. */
  def getCompleted(
    engineId: String,
    engineVersion: String,
    engineVariant: String): Seq[EngineInstance] = DB localTx { implicit s =>
    try {
      sql"""
      SELECT
        id,
        status,
        starttime,
        endtime,
        engineid,
        engineversion,
        enginevariant,
        enginefactory,
        evaluatorclass,
        batch,
        env,
        sparkconf,
        datasourceparams,
        preparatorparams,
        algorithmsparams,
        servingparams,
        evaluatorparams,
        evaluatorresults,
        evaluatorresultshtml,
        evaluatorresultsjson
      FROM engineinstances
      WHERE
        status = 'COMPLETED' AND
        engineid = $engineId AND
        engineversion = $engineVersion AND
        enginevariant = $engineVariant
      ORDER BY starttime DESC""".
        map(resultToEngineInstance).list().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        Seq()
    }
  }

  def getEvalCompleted(): Seq[EngineInstance] = DB localTx { implicit s =>
    try {
      sql"""
      SELECT
        id,
        status,
        starttime,
        endtime,
        engineid,
        engineversion,
        enginevariant,
        enginefactory,
        evaluatorclass,
        batch,
        env,
        sparkconf,
        datasourceparams,
        preparatorparams,
        algorithmsparams,
        servingparams,
        evaluatorparams,
        evaluatorresults,
        evaluatorresultshtml,
        evaluatorresultsjson
      FROM engineinstances
      WHERE
        status = 'EVALCOMPLETED'
      ORDER BY starttime DESC""".
        map(resultToEngineInstance).list().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        Seq()
    }
  }

  /** Update a EngineInstance. */
  def update(i: EngineInstance): Unit = DB localTx { implicit session =>
    try {
      sql"""
      update engineinstances set
        status = ${i.status},
        starttime = ${i.startTime},
        endtime = ${i.endTime},
        engineid = ${i.engineId},
        engineversion = ${i.engineVersion},
        enginevariant = ${i.engineVariant},
        enginefactory = ${i.engineFactory},
        evaluatorclass = ${i.evaluatorClass},
        batch = ${i.batch},
        env = ${JDBCUtils.mapToString(i.env)},
        sparkconf = ${JDBCUtils.mapToString(i.sparkConf)},
        datasourceparams = ${i.dataSourceParams},
        preparatorparams = ${i.preparatorParams},
        algorithmsparams = ${i.algorithmsParams},
        servingparams = ${i.servingParams},
        evaluatorparams = ${i.evaluatorParams},
        evaluatorresults = ${i.evaluatorResults},
        evaluatorresultshtml = ${i.evaluatorResultsHTML},
        evaluatorresultsjson = ${i.evaluatorResultsJSON}
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
      sql"DELETE FROM engineinstances WHERE id = $id".update().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
    }
  }

  def resultToEngineInstance(rs: WrappedResultSet): EngineInstance = {
    EngineInstance(
      id = rs.string("id"),
      status = rs.string("status"),
      startTime = rs.jodaDateTime("starttime"),
      endTime = rs.jodaDateTime("endtime"),
      engineId = rs.string("engineid"),
      engineVersion = rs.string("engineversion"),
      engineVariant = rs.string("enginevariant"),
      engineFactory = rs.string("enginefactory"),
      evaluatorClass = rs.string("evaluatorclass"),
      batch = rs.string("batch"),
      env = JDBCUtils.stringToMap(rs.string("env")),
      sparkConf = JDBCUtils.stringToMap(rs.string("sparkconf")),
      dataSourceParams = rs.string("datasourceparams"),
      preparatorParams = rs.string("preparatorparams"),
      algorithmsParams = rs.string("algorithmsparams"),
      servingParams = rs.string("servingparams"),
      evaluatorParams = rs.string("evaluatorparams"),
      evaluatorResults = rs.string("evaluatorresults"),
      evaluatorResultsHTML = rs.string("evaluatorresultshtml"),
      evaluatorResultsJSON = rs.string("evaluatorresultsjson"))
  }
}
