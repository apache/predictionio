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

package io.prediction.tools.dashboard

import io.prediction.data.storage.Storage

import akka.actor.{ActorContext, Actor, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.nscala_time.time.Imports.DateTime
import grizzled.slf4j.Logging
import spray.can.Http
import spray.http._
import spray.http.MediaTypes._
import spray.routing._

import scala.concurrent.duration._

case class DashboardConfig(
  ip: String = "localhost",
  port: Int = 9000)

object Dashboard extends Logging {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[DashboardConfig]("Dashboard") {
      opt[String]("ip") action { (x, c) =>
        c.copy(ip = x)
      } text("IP to bind to (default: localhost).")
      opt[Int]("port") action { (x, c) =>
        c.copy(port = x)
      } text("Port to bind to (default: 9000).")
    }

    parser.parse(args, DashboardConfig()) map { dc =>
      createDashboard(dc)
    }
  }

  def createDashboard(dc: DashboardConfig): Unit = {
    implicit val system = ActorSystem("pio-dashboard")
    val service =
      system.actorOf(Props(classOf[DashboardActor], dc), "dashboard")
    implicit val timeout = Timeout(5.seconds)
    IO(Http) ? Http.Bind(service, interface = dc.ip, port = dc.port)
    system.awaitTermination
  }
}

class DashboardActor(
    val dc: DashboardConfig)
  extends Actor with DashboardService {
  def actorRefFactory: ActorContext = context
  def receive: Actor.Receive = runRoute(dashboardRoute)
}

trait DashboardService extends HttpService with CORSSupport {
  val dc: DashboardConfig
  val evaluationInstances = Storage.getMetaDataEvaluationInstances
  val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_"))
  val serverStartTime = DateTime.now
  val dashboardRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            val completedInstances = evaluationInstances.getCompleted
            html.index(
              dc,
              serverStartTime,
              pioEnvVars,
              completedInstances).toString
          }
        }
      }
    } ~
    pathPrefix("engine_instances" / Segment) { instanceId =>
      path("evaluator_results.txt") {
        get {
          respondWithMediaType(`text/plain`) {
            evaluationInstances.get(instanceId).map { i =>
              complete(i.evaluatorResults)
            } getOrElse {
              complete(StatusCodes.NotFound)
            }
          }
        }
      } ~
      path("evaluator_results.html") {
        get {
          respondWithMediaType(`text/html`) {
            evaluationInstances.get(instanceId).map { i =>
              complete(i.evaluatorResultsHTML)
            } getOrElse {
              complete(StatusCodes.NotFound)
            }
          }
        }
      } ~
      path("evaluator_results.json") {
        get {
          respondWithMediaType(`application/json`) {
            evaluationInstances.get(instanceId).map { i =>
              complete(i.evaluatorResultsJSON)
            } getOrElse {
              complete(StatusCodes.NotFound)
            }
          }
        }
      } ~
      cors {
        path("local_evaluator_results.json") {
          get {
            respondWithMediaType(`application/json`) {
              evaluationInstances.get(instanceId).map { i =>
                complete(i.evaluatorResultsJSON)
              } getOrElse {
                complete(StatusCodes.NotFound)
              }
            }
          }
        }
      }
    } ~
    pathPrefix("assets") {
      getFromResourceDirectory("assets")
    }
}
