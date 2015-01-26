package io.prediction.data.api

import grizzled.slf4j.Logging
import io.prediction.data.storage.{AccessKey, App, Storage}

import scala.concurrent.{ExecutionContext, Future}

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


object CmdClient extends Logging {
  def getFutureApps()(implicit ec: ExecutionContext): Future[Seq[AppResponse]] = Future {
    val apps = Storage.getMetaDataApps.getAll().sortBy(_.name)
    val accessKeys = Storage.getMetaDataAccessKeys

    val appsRes = apps.map {
      app => {
        new AppResponse(1, "Successful retrieved app list.", app.id, app.name, accessKeys.getByAppid(app.id))
      }
    }
    appsRes
  }

  def deleteAppData(appName: String)(implicit ec: ExecutionContext): Future[GeneralResponse] = Future {
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
      events.close()
      GeneralResponse(data.status + data2.status, data.message + data2.message)
    } getOrElse {
      GeneralResponse(0, s"App ${appName} does not exist.")
    }
    response
  }

  def deleteApp(appName: String)(implicit ec: ExecutionContext): Future[GeneralResponse] = Future {
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
      events.close()
      data
    } getOrElse {
      GeneralResponse(0, s"App ${appName} does not exist.")
    }
    response
  }

  def newApp(appArgs: AppRequest)(implicit ec: ExecutionContext): Future[BaseResponse] = Future {
    val apps = Storage.getMetaDataApps
    val response = apps.getByName(appArgs.name) map { app =>
      GeneralResponse(0, s"App ${appArgs.name} already exists. Aborting.")
    } getOrElse {
      apps.get(appArgs.id) map {
        app2 =>
          GeneralResponse(0, s"App ID ${app2.id} already exists and maps to the app '${app2.name}'. " +
            "Aborting.")
      } getOrElse {
        val appid = apps.insert(App(
          id = Option(appArgs.id).getOrElse(0),
          name = appArgs.name,
          description = Option(appArgs.description)))
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
              new AppResponse(1,"App created successfuly.",id, appArgs.name, Seq[AccessKey](accessKey))
            } getOrElse {
              GeneralResponse(0, s"Unable to create new access key.")
            }
          } else {
            GeneralResponse(0, s"Unable to initialize Event Store for this app ID: ${id}.")
          }
          events.close()
          r
        } getOrElse {
          GeneralResponse(0, s"Unable to create new app.")
        }
      }
    }
    response
  }
}
