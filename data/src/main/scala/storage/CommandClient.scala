/** Copyright 2014 TappingStone, Inc.
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

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext

abstract class BaseResponse(s: Int, m: String) {
  def getStatus = s
  def getMessage = m
}

case class GeneralResponse(
                            status: Int = 0,
                            message: String = "") extends BaseResponse(status,message)

case class AppRequest(
                       id: Int = 0,
                       name: String = "",
                       description: String = ""
                       )

case class AppResponse(
                        status: Int = 0,
                        message: String = "",
                        id: Int = 0,
                        name: String = "",
                        keys: Seq[AccessKey]
                        ) extends BaseResponse(status,message)


case class AppListResponse(
                        status: Int = 0,
                        message: String = "",
                        apps: Seq[AppResponse]
                            ) extends BaseResponse(status,message)

// see Console.scala for reference

class CommandClient (
  val appClient: Apps,
  val accessKeyClient: AccessKeys,
  val eventClient: LEvents
) {

  // see def appNew() in Console.scala
  def futureAppNew(req: AppRequest)
    (implicit ec: ExecutionContext): Future[BaseResponse] = Future {
    val apps = Storage.getMetaDataApps
    val response = apps.getByName(req.name) map { app =>
      GeneralResponse(0, s"App ${req.name} already exists. Aborting.")
    } getOrElse {
      apps.get(req.id) map {
        app2 =>
          GeneralResponse(0, s"App ID ${app2.id} already exists and maps to the app '${app2.name}'. " +
            "Aborting.")
      } getOrElse {
        val appid = apps.insert(App(
          id = Option(req.id).getOrElse(0),
          name = req.name,
          description = Option(req.description)))
        appid map { id =>
          val events = Storage.getLEvents()
          val dbInit = events.init(id)
          val r = if (dbInit) {

            val accessKeys = Storage.getMetaDataAccessKeys
            val accessKey = AccessKey(
              key = "",
              appid = id,
              events = Seq())

            val accessKey2 = accessKeys.insert(AccessKey(
              key = "",
              appid = id,
              events = Seq()))

            accessKey2 map { k =>
              new AppResponse(1,"App created successfuly.",id, req.name, Seq[AccessKey](accessKey))
            } getOrElse {
              GeneralResponse(0, s"Unable to create new access key.")
            }
          } else {
            GeneralResponse(0, s"Unable to initialize Event Store for this app ID: ${id}.")
          }
          r
        } getOrElse {
          GeneralResponse(0, s"Unable to create new app.")
        }
      }
    }
    response
  }

  // see def appList() in Console.scala
  def futureAppList()
    (implicit ec: ExecutionContext): Future[AppListResponse] = Future {
    val apps = Storage.getMetaDataApps.getAll().sortBy(_.name)
    val accessKeys = Storage.getMetaDataAccessKeys

    val appsRes = apps.map {
      app => {
        new AppResponse(1, "Successful retrieved app.", app.id, app.name, accessKeys.getByAppid(app.id))
      }
    }
    new AppListResponse(1,"Successful retrieved app list.",appsRes)
  }

  def futureAppDataDelete(appName: String)(implicit ec: ExecutionContext): Future[GeneralResponse] = Future {
    val apps = Storage.getMetaDataApps
    val events = Storage.getLEvents()

    val response = apps.getByName(appName) map { app =>
      val data = if (events.remove(app.id)) {
        GeneralResponse(1, s"Removed Event Store for this app ID: ${app.id}")
      } else {
        GeneralResponse(0, s"Error removing Event Store for this app.")
      }

      val dbInit = events.init(app.id)
      val data2 = if (dbInit) {
        GeneralResponse(1, s"Initialized Event Store for this app ID: ${app.id}.")
      } else {
        GeneralResponse(0, s"Unable to initialize Event Store for this appId:" +
          s" ${app.id}.")
      }
      GeneralResponse(data.status * data2.status, data.message + data2.message)
    } getOrElse {
      GeneralResponse(0, s"App ${appName} does not exist.")
    }
    response
  }

  def futureAppDelete(appName: String)(implicit ec: ExecutionContext): Future[GeneralResponse] = Future {
    val apps = Storage.getMetaDataApps
    val events = Storage.getLEvents()

    val response = apps.getByName(appName) map { app =>
      val data = if (events.remove(app.id)) {
        if (Storage.getMetaDataApps.delete(app.id)) {
          GeneralResponse(1, s"App successfully deleted")
        } else {
          GeneralResponse(0, s"Error deleting app ${app.name}.")
        }
      } else {
        GeneralResponse(0, s"Error removing Event Store for app ${app.name}.");
      }
      data
    } getOrElse {
      GeneralResponse(0, s"App ${appName} does not exist.")
    }
    response
  }

}
