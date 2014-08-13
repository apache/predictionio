package io.prediction.tools.dashboard

import io.prediction.storage.Storage

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
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
  def actorRefFactory = context
  def receive = runRoute(dashboardRoute)
}

trait DashboardService extends HttpService {
  val dc: DashboardConfig
  val engineInstances = Storage.getMetaDataEngineInstances
  val pioEnvVars = sys.env.filter(kv => kv._1.startsWith("PIO_"))
  val dashboardRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            val evalCompletedInstances = engineInstances.getEvalCompleted
            html.index(
              dc,
              DateTime.now,
              pioEnvVars,
              evalCompletedInstances).toString
          }
        }
      }
    } ~
    pathPrefix("engine_instances"/ Segment) { instanceId =>
      path("metrics_results.txt") {
        get {
          respondWithMediaType(`text/plain`) {
            engineInstances.get(instanceId).map { i =>
              complete(i.multipleMetricsResults)
            } getOrElse {
              complete(StatusCodes.NotFound)
            }
          }
        }
      } ~
      path("metrics_results.html") {
        get {
          respondWithMediaType(`text/html`) {
            engineInstances.get(instanceId).map { i =>
              complete(i.multipleMetricsResults)
            } getOrElse {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    } ~
    pathPrefix("assets") {
      getFromResourceDirectory("assets")
    }
}
