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

import akka.actor.ActorSelection
import akka.event.LoggingAdapter
import io.prediction.data.storage.LEvents
import io.prediction.data.webhooks.ConnectorUtil
import org.json4s.JObject
import spray.http.{FormData, StatusCode, StatusCodes}

import scala.concurrent.{ExecutionContext, Future}


private[prediction] object Webhooks {

  def postJson(
    appId: Int,
    channelId: Option[Int],
    web: String,
    data: JObject,
    eventClient: LEvents,
    log: LoggingAdapter,
    stats: Boolean,
    statsActorRef: ActorSelection
  )(implicit ec: ExecutionContext): Future[(StatusCode, Map[String, String])] = {

    val eventFuture = Future {
      WebhooksConnectors.json.get(web).map(ConnectorUtil.toEvent(_, data))
    }

    eventFuture.flatMap { eventOpt =>
      if (eventOpt.isEmpty) {
        Future successful {
          val message = s"webhooks connection for $web is not supported."
          (StatusCodes.NotFound, Map("message" -> message))
        }
      } else {
        eventOpt.get.fold(
          error ⇒
            Future successful {
              (StatusCodes.BadRequest: StatusCode, Map("error" → error))
            },
          event ⇒
            eventClient.futureInsert(event, appId, channelId).map { id =>
              if (stats) {
                statsActorRef ! Bookkeeping(appId, StatusCodes.Created, event)
              }

              (StatusCodes.Created, Map("eventId" -> s"${id }"))
            }
        )
      }
    }
  }

  def getJson(
    appId: Int,
    channelId: Option[Int],
    web: String,
    log: LoggingAdapter
  )(implicit ec: ExecutionContext): Future[(StatusCode, Map[String, String])] = {
    Future {
      WebhooksConnectors.json.get(web).map { connector =>
        (StatusCodes.OK, Map("message" -> "Ok"))
      }.getOrElse {
        val message = s"webhooks connection for ${web} is not supported."
        (StatusCodes.NotFound, Map("message" -> message))
      }
    }
  }

  def postForm(
    appId: Int,
    channelId: Option[Int],
    web: String,
    data: FormData,
    eventClient: LEvents,
    log: LoggingAdapter,
    stats: Boolean,
    statsActorRef: ActorSelection
  )(implicit ec: ExecutionContext): Future[(StatusCode, Map[String, String])] = {
    val eventFuture = Future {
      WebhooksConnectors.form.get(web).map { connector =>
        ConnectorUtil.toEvent(connector, data.fields.toMap)
      }
    }

    eventFuture.flatMap { eventOpt =>
      if (eventOpt.isEmpty) {
        Future {
          val message = s"webhooks connection for ${web} is not supported."
          (StatusCodes.NotFound, Map("message" -> message))
        }
      } else {
        val event = eventOpt.get
        val data = eventClient.futureInsert(event, appId, channelId).map { id =>
          val result = (StatusCodes.Created, Map("eventId" -> s"${id}"))

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
    channelId: Option[Int],
    web: String,
    log: LoggingAdapter
  )(implicit ec: ExecutionContext): Future[(StatusCode, Map[String, String])] = {
    Future {
      WebhooksConnectors.form.get(web).map { connector =>
        (StatusCodes.OK, Map("message" -> "Ok"))
      }.getOrElse {
        val message = s"webhooks connection for ${web} is not supported."
        (StatusCodes.NotFound, Map("message" -> message))
      }
    }
  }

}
