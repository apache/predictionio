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

package io.prediction.tools.console

import io.prediction.data.storage.{AccessKey => StorageAccessKey}
import io.prediction.data.storage.{App => StorageApp}
import io.prediction.data.storage.Storage

import grizzled.slf4j.Logging

case class AppArgs(
  id: Option[Int] = None,
  name: String = "",
  description: Option[String] = None)

object App extends Logging {
  def create(ca: ConsoleArgs): Int = {
    val apps = Storage.getMetaDataApps
    apps.getByName(ca.app.name) map { app =>
      error(s"App ${ca.app.name} already exists. Aborting.")
      1
    } getOrElse {
      ca.app.id.map { id =>
        apps.get(id) map { app =>
          error(
            s"App ID ${id} already exists and maps to the app '${app.name}'. " +
            "Aborting.")
          return 1
        }
      }
      val appid = apps.insert(StorageApp(
        id = ca.app.id.getOrElse(0),
        name = ca.app.name,
        description = ca.app.description))
      appid map { id =>
        val events = Storage.getLEvents()
        val dbInit = events.init(id)
        val r = if (dbInit) {
          info(s"Initialized Event Store for this app ID: ${id}.")
          val accessKeys = Storage.getMetaDataAccessKeys
          val accessKey = accessKeys.insert(StorageAccessKey(
            key = "",
            appid = id,
            events = Seq()))
          accessKey map { k =>
            info("Created new app:")
            info(s"      Name: ${ca.app.name}")
            info(s"        ID: ${id}")
            info(s"Access Key: ${k}")
            0
          } getOrElse {
            error(s"Unable to create new access key.")
            1
          }
        } else {
          error(s"Unable to initialize Event Store for this app ID: ${id}.")
          1
        }
        events.close()
        r
      } getOrElse {
        error(s"Unable to create new app.")
        1
      }
    }
  }

  def list(ca: ConsoleArgs): Int = {
    val apps = Storage.getMetaDataApps.getAll().sortBy(_.name)
    val accessKeys = Storage.getMetaDataAccessKeys
    val title = "Name"
    val ak = "Access Key"
    info(f"$title%20s |   ID | $ak%64s | Allowed Event(s)")
    apps foreach { app =>
      val keys = accessKeys.getByAppid(app.id)
      keys foreach { k =>
        val events =
          if (k.events.size > 0) k.events.sorted.mkString(",") else "(all)"
        info(f"${app.name}%20s | ${app.id}%4d | ${k.key}%s | ${events}%s")
      }
    }
    info(s"Finished listing ${apps.size} app(s).")
    0
  }

  def delete(ca: ConsoleArgs): Int = {
    val apps = Storage.getMetaDataApps
    apps.getByName(ca.app.name) map { app =>
      info(s"The following app will be deleted. Are you sure?")
      info(s"    App Name: ${app.name}")
      info(s"      App ID: ${app.id}")
      info(s" Description: ${app.description}")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          val events = Storage.getLEvents()
          val r = if (events.remove(app.id)) {
            info(s"Removed Event Store for this app ID: ${app.id}")
            if (Storage.getMetaDataApps.delete(app.id)) {
              info(s"Deleted app ${app.name}.")
              0
            } else {
              error(s"Error deleting app ${app.name}.")
              1
            }
          } else {
            error(s"Error removing Event Store for this app.")
            1
          }
          events.close()
          info("Done.")
          r
        }
        case _ =>
          info("Aborted.")
          0
      }
    } getOrElse {
      error(s"App ${ca.app.name} does not exist. Aborting.")
      1
    }
  }

  def dataDelete(ca: ConsoleArgs): Int = {
    val apps = Storage.getMetaDataApps
    apps.getByName(ca.app.name) map { app =>
      info(s"The data of the following app will be deleted. Are you sure?")
      info(s"    App Name: ${app.name}")
      info(s"      App ID: ${app.id}")
      info(s" Description: ${app.description}")
      val choice = readLine("Enter 'YES' to proceed: ")
      choice match {
        case "YES" => {
          val events = Storage.getLEvents()
          // remove table
          val r1 = if (events.remove(app.id)) {
            info(s"Removed Event Store for this app ID: ${app.id}")
            0
          } else {
            error(s"Error removing Event Store for this app.")
            1
          }
          // re-create table
          val dbInit = events.init(app.id)
          val r2 = if (dbInit) {
            info(s"Initialized Event Store for this app ID: ${app.id}.")
            0
          } else {
            error(s"Unable to initialize Event Store for this appId:" +
              s" ${app.id}.")
            1
          }
          events.close()
          info("Done.")
          r1 + r2
        }
        case _ =>
          info("Aborted.")
          0
      }
    } getOrElse {
      error(s"App ${ca.app.name} does not exist. Aborting.")
      1
    }
  }
}
