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

package io.prediction.data.api

import io.prediction.data.webhooks.WebhooksJsonConverter
import io.prediction.data.webhooks.WebhooksFormConverter
import io.prediction.data.storage.LEvents
import io.prediction.data.storage.StorageError

import spray.routing._
import spray.routing.Directives._
import spray.http.StatusCodes
import spray.http.StatusCode
import spray.http.FormData
import spray.httpx.Json4sSupport

import org.json4s.{Formats, DefaultFormats}
import org.json4s.JObject

import akka.event.LoggingAdapter
import akka.actor.ActorSelection

import scala.concurrent.{ExecutionContext, Future}


object Webhooks {

  /*private object Json4sProtocol extends Json4sSupport {
    implicit def json4sFormats: Formats = DefaultFormats
  }*/

  def postJson(
    appId: Int,
    jObj: JObject,
    web: String,
    webhooksJsonConverters: Map[String, WebhooksJsonConverter],
    eventClient: LEvents,
    log: LoggingAdapter,
    stats: Boolean,
    statsActorRef: ActorSelection
  )(implicit ec: ExecutionContext): Future[(StatusCode, Map[String, String])] = {

    val eventFuture = Future {
      webhooksJsonConverters.get(web).map { converter =>
        converter.convert(jObj)
      }
    }

    eventFuture.flatMap { eventOpt =>
      if (eventOpt.isEmpty) {
        Future {
          log.info(s"invalid ${web} path")
          val message = s"webhooks for ${web} is not supported."
          (StatusCodes.NotFound, Map("message" -> message))
        }
      } else {
        val event = eventOpt.get
        val data = eventClient.futureInsert(event, appId).map { r =>
          val result = r match {
            case Left(StorageError(message)) =>
              (StatusCodes.InternalServerError,
                Map("message" -> message))
            case Right(id) =>
              (StatusCodes.Created, Map("eventId" -> s"${id}"))
          }
          if (stats) {
            statsActorRef ! Bookkeeping(appId, result._1, event)
          }
          result
        }
        data
      }
    }
  }

  def getJson(
    appId: Int,
    web: String,
    webhooksJsonConverters: Map[String, WebhooksJsonConverter],
    log: LoggingAdapter
  )(implicit ec: ExecutionContext): Future[(StatusCode, Map[String, String])] = {
    Future {
      webhooksJsonConverters.get(web).map { converter =>
        (StatusCodes.OK, Map("message" -> "Ok"))
      }.getOrElse {
        val message = s"webhooks for ${web} is not supported."
        (StatusCodes.NotFound, Map("message" -> message))
      }
    }
  }

  def postForm(
    appId: Int,
    formData: FormData,
    web: String,
    webhooksFormConverters: Map[String, WebhooksFormConverter],
    eventClient: LEvents,
    log: LoggingAdapter,
    stats: Boolean,
    statsActorRef: ActorSelection
  )(implicit ec: ExecutionContext): Future[(StatusCode, String)] = {
    val eventFuture = Future {
      webhooksFormConverters.get(web).map { converter =>
        converter.convert(formData.fields.toMap)
      }
    }

    eventFuture.flatMap { eventOpt =>
      if (eventOpt.isEmpty) {
        Future {
          log.info(s"invalid ${web} path")
          val message = s"webhooks for ${web} is not supported."
          (StatusCodes.NotFound, message)
        }
      } else {
        val event = eventOpt.get
        val data = eventClient.futureInsert(event, appId).map { r =>
          val result = r match {
            case Left(StorageError(message)) =>
              (StatusCodes.InternalServerError, message)
            case Right(id) =>
              (StatusCodes.Created, (s"eventId is ${id}"))
          }
          if (stats) {
            statsActorRef ! Bookkeeping(appId, result._1, event)
          }
          result
        }
        data
      }
    }
  }

  def getForm(
    appId: Int,
    web: String,
    webhooksFormConverters: Map[String, WebhooksFormConverter],
    log: LoggingAdapter
  )(implicit ec: ExecutionContext): Future[(StatusCode, String)] = {
    Future {
      webhooksFormConverters.get(web).map { converter =>
        (StatusCodes.OK, "Ok")
      }.getOrElse {
        val message = s"webhooks for ${web} is not supported."
        (StatusCodes.NotFound, message)
      }
    }
  }

/*
  def route(log: LoggingAdapter) =
    path("webhooks" / "test") {
      post {
        entity(as[FormData]){ m =>
          println(m)
          log.info(s"${m} ${m.fields} ${m.fields.size}")
          val d = m.fields.foreach { case (s1, s2) =>
            log.info(s"s1 ${s1}  s2 ${s2}")
          }

          complete(s"The color is ${m}")
        }
      }
    } ~
    path ("webhooks" / "testjon") {

      import Json4sProtocol._

      post {
        entity(as[JObject]) { jObj =>
          println(jObj)
          log.info(s"${jObj}")
          complete("json request")
        }
      }
    }
*/

}
