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
import io.prediction.data.storage.EngineManifest
import io.prediction.data.storage.EngineManifests
import io.prediction.data.storage.StorageClientConfig
import scalikejdbc._

class JDBCEngineManifests(client: String, config: StorageClientConfig, prefix: String)
  extends EngineManifests with Logging {
  val tableName = JDBCUtils.prefixTableName(prefix, "enginemanifests")
  DB autoCommit { implicit session =>
    try {
      sql"""
      create table $tableName (
        id text not null primary key,
        version text not null,
        name text not null,
        description text,
        files text not null,
        enginefactory text not null)""".execute().apply()
    } catch {
      case e: Exception => debug(e.getMessage, e)
    }
  }

  def insert(m: EngineManifest): Unit = DB localTx { implicit session =>
    try {
      sql"""
      INSERT INTO $tableName VALUES(
        ${m.id},
        ${m.version},
        ${m.name},
        ${m.description},
        ${m.files.mkString(",")},
        ${m.engineFactory})""".update().apply()
    } catch {
      case e: Exception => error(e.getMessage, e)
    }
  }

  def get(id: String, version: String): Option[EngineManifest] = DB localTx { implicit session =>
    try {
      sql"""
      SELECT
        id,
        version,
        name,
        description,
        files,
        enginefactory
      FROM $tableName WHERE id = $id AND version = $version""".
        map(resultToEngineManifest).single().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        None
    }
  }

  def getAll(): Seq[EngineManifest] = DB localTx { implicit session =>
    try {
      sql"""
      SELECT
        id,
        version,
        name,
        description,
        files,
        enginefactory
      FROM $tableName""".map(resultToEngineManifest).list().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        Seq()
    }
  }

  def update(m: EngineManifest, upsert: Boolean = false): Unit = DB localTx { implicit session =>
    try {
      val r = sql"""
      update $tableName set
        name = ${m.name},
        description = ${m.description},
        files = ${m.files.mkString(",")},
        enginefactory = ${m.engineFactory}
      where id = ${m.id} and version = ${m.version}""".update().apply()
      if (r == 0) {
        if (upsert)
          insert(m)
        else
          error("Cannot find a record to update, and upsert is not enabled.")
      }
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        ""
    }
  }

  def delete(id: String, version: String): Unit = DB localTx { implicit session =>
    try {
      sql"DELETE FROM $tableName WHERE id = $id AND version = $version".
        update().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
    }
  }

  def resultToEngineManifest(rs: WrappedResultSet): EngineManifest = {
    EngineManifest(
      id = rs.string("id"),
      version = rs.string("version"),
      name = rs.string("name"),
      description = rs.stringOpt("description"),
      files = rs.string("files").split(","),
      engineFactory = rs.string("enginefactory"))
  }
}
