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

import scalikejdbc._

object JDBCUtils {
  def driverType(url: String): String = {
    val capture = """jdbc:([^:]+):""".r
    capture findFirstIn url match {
      case Some(capture(driverType)) => driverType
      case None => ""
    }
  }

  def autoIncrementType(url: String): String = {
    driverType(url) match {
      case "postgresql" => "SERIAL"
      case _ => ""
    }
  }

  def mapToString(m: Map[String, String]): String = {
    m.map(t => s"${t._1}=${t._2}").mkString(",")
  }

  def stringToMap(str: String): Map[String, String] = {
    str.split(",").map { x =>
      val y = x.split("=")
      y(0) -> y(1)
    }.toMap[String, String]
  }

  def generateId: String = java.util.UUID.randomUUID().toString.replace("-", "")

  def prefixTableName(prefix: String, table: String): SQLSyntax =
    sqls.createUnsafely(s"${prefix}_$table")

  def eventTableName(namespace: String, appId: Int, channelId: Option[Int]): String =
    s"${namespace}_${appId}${channelId.map("_" + _).getOrElse("")}"
}
