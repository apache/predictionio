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
import io.prediction.data.storage.Channel
import io.prediction.data.storage.Channels
import io.prediction.data.storage.StorageClientConfig
import scalikejdbc._

class JDBCChannels(client: String, config: StorageClientConfig, prefix: String)
  extends Channels with Logging {
  val tableName = JDBCUtils.prefixTableName(prefix, "channels")
  DB autoCommit { implicit session =>
    try {
      sql"""
      create table $tableName (
        id serial not null primary key,
        name text not null,
        appid integer not null)""".execute().apply()
    } catch {
      case e: Exception => debug(e.getMessage, e) // assume table already exists
    }
  }

  def insert(channel: Channel): Option[Int] = DB localTx { implicit session =>
    try {
      val q = if (channel.id == 0)
        sql"INSERT INTO $tableName (name, appid) VALUES(${channel.name}, ${channel.appid})"
      else
        sql"INSERT INTO $tableName VALUES(${channel.id}, ${channel.name}, ${channel.appid})"
      Some(q.updateAndReturnGeneratedKey().apply().toInt)
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        None
    }
  }

  def get(id: Int): Option[Channel] = DB localTx { implicit session =>
    try {
      sql"SELECT id, name, appid FROM $tableName WHERE id = $id".
        map(resultToChannel).single().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        None
    }
  }

  def getByAppid(appid: Int): Seq[Channel] = DB localTx { implicit session =>
    try {
      sql"SELECT id, name, appid FROM $tableName WHERE appid = $appid".
        map(resultToChannel).list().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        Seq()
    }
  }

  def update(channel: Channel): Boolean = DB localTx { implicit session =>
    try {
      sql"""
      UPDATE $tableName SET
        name = ${channel.name},
        appid = ${channel.appid}
      WHERE id = ${channel.id}""".update().apply()
      true
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        false
    }
  }

  def delete(id: Int): Boolean = DB localTx { implicit session =>
    try {
      sql"DELETE FROM $tableName WHERE id = $id".update().apply()
      true
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        false
    }
  }

  def resultToChannel(rs: WrappedResultSet): Channel = {
    Channel(
      id = rs.int("id"),
      name = rs.string("name"),
      appid = rs.int("appid"))
  }
}
