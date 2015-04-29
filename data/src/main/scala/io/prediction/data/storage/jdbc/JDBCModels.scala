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
import io.prediction.data.storage.Model
import io.prediction.data.storage.Models
import io.prediction.data.storage.StorageClientConfig
import scalikejdbc._

class JDBCModels(client: String, config: StorageClientConfig, prefix: String)
  extends Models with Logging {
  val tableName = JDBCUtils.prefixTableName(prefix, "models")
  DB autoCommit { implicit session =>
    try {
      sql"""
      create table $tableName (
        id text not null primary key,
        models bytea not null)""".execute().apply()
    } catch {
      case e: Exception => debug(e.getMessage, e)
    }
  }

  def insert(i: Model): Unit = DB localTx { implicit session =>
    try {
      sql"insert into $tableName values(${i.id}, ${i.models})".update().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
    }
  }

  def get(id: String): Option[Model] = DB readOnly { implicit session =>
    try {
      sql"select id, models from $tableName where id = $id".map { r =>
        Model(id = r.string("id"), models = r.bytes("models"))
      }.single().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
        None
    }
  }

  def delete(id: String): Unit = DB localTx { implicit session =>
    try {
      sql"delete from $tableName where id = $id".execute().apply()
    } catch {
      case e: Exception =>
        error(e.getMessage, e)
    }
  }
}
