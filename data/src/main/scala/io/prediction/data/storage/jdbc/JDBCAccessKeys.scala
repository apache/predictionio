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
import io.prediction.data.storage.AccessKey
import io.prediction.data.storage.AccessKeys
import io.prediction.data.storage.StorageClientConfig
import scalikejdbc._

import scala.util.Random

/** JDBC implementation of AccessKeys. */
class JDBCAccessKeys(client: String, config: StorageClientConfig, prefix: String)
  extends AccessKeys with Logging {
  val tableName = JDBCUtils.prefixTableName(prefix, "accesskeys")
  DB autoCommit { implicit session =>
    sql"""
    create table if not exists $tableName (
      accesskey varchar(64) not null primary key,
      appid integer not null,
      events text)""".execute().apply()
  }

  def insert(accessKey: AccessKey): Option[String] = DB localTx { implicit s =>
    val generatedkey = Random.alphanumeric.take(64).mkString
    sql"""
    insert into $tableName values(
      $generatedkey,
      ${accessKey.appid},
      ${accessKey.events.mkString(",")})""".update().apply()
    Some(generatedkey)
  }

  def get(key: String): Option[AccessKey] = DB readOnly { implicit session =>
    sql"SELECT accesskey, appid, events FROM $tableName WHERE accesskey = $key".
      map(resultToAccessKey).single().apply()
  }

  def getAll(): Seq[AccessKey] = DB readOnly { implicit session =>
    sql"SELECT accesskey, appid, events FROM $tableName".map(resultToAccessKey).list().apply()
  }

  def getByAppid(appid: Int): Seq[AccessKey] = DB readOnly { implicit session =>
    sql"SELECT accesskey, appid, events FROM $tableName WHERE appid = $appid".
      map(resultToAccessKey).list().apply()
  }

  def update(accessKey: AccessKey): Boolean = DB localTx { implicit session =>
    sql"""
    UPDATE $tableName SET
      appid = ${accessKey.appid},
      events = ${accessKey.events.mkString(",")}
    WHERE accesskey = ${accessKey.key}""".update().apply()
    true
  }

  def delete(key: String): Boolean = DB localTx { implicit session =>
    sql"DELETE FROM $tableName WHERE accesskey = $key".update().apply()
    true
  }

  def resultToAccessKey(rs: WrappedResultSet): AccessKey = {
    AccessKey(
      key = rs.string("accesskey"),
      appid = rs.int("appid"),
      events = rs.string("events").split(","))
  }
}
