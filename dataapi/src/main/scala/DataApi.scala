package io.prediction.dataapi

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.io.IO
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{ read, write }
import org.json4s.ext.JodaTimeSerializers

import spray.http.StatusCodes
import spray.http.MediaTypes
import spray.http.HttpResponse
import spray.httpx.Json4sSupport
import spray.can.Http
import spray.routing._
import Directives._

class DataServiceActor(val eventClient: Events) extends HttpServiceActor {

  object Json4sProtocol extends Json4sSupport {
    implicit def json4sFormats: Formats = DefaultFormats.lossless ++
      JodaTimeSerializers.all
  }

  import Json4sProtocol._

  val log = Logging(context.system, this)

  //val eventClient = Storage.eventClient

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our
  // Futures
  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val testServiceActor = actorRefFactory.actorOf(Props[TestServiceActor],
    "TestServiceActor")

  val route: Route =
    pathSingleSlash {
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete("status" -> "alive")
        }
      }
    } ~
    path("events" / Segment) { eventId =>
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete {
            log.info(s"GET event ${eventId}.")
            val data = eventClient.futureGet(eventId).map { r =>
              r match {
                case Left(StorageError(message)) =>
                  (StatusCodes.InternalServerError, ("message" -> message))
                case Right(eventOpt) => {
                  eventOpt.map( event =>
                    (StatusCodes.OK, event)
                  ).getOrElse(
                    (StatusCodes.NotFound, None)
                  )
                }
              }
            }
            data
          }
        }
      } ~
      delete {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete {
            log.info(s"DELETE event ${eventId}.")
            val data = eventClient.futureDelete(eventId).map { r =>
              r match {
                case Left(StorageError(message)) =>
                  (StatusCodes.InternalServerError, ("message" -> message))
                case Right(found) =>
                  if (found) {
                    (StatusCodes.OK, ("found" -> found))
                  } else {
                    (StatusCodes.NotFound, None)
                  }
              }
            }
            data
          }
        }
      }
    } ~
    path("events") {
      post {
        entity(as[Event]) { event =>
          //val event = jsonObj.extract[Event]
          complete {
            log.info(s"POST events")
            val data = eventClient.futureInsert(event).map { r =>
              r match {
                case Left(StorageError(message)) =>
                  (StatusCodes.InternalServerError, ("message" -> message))
                case Right(id) =>
                  (StatusCodes.Created, ("eventId" -> s"${id}"))
              }
            }
            data
          }
        }
      } ~
      get {
        parameter('appId.as[Int]) { appId =>
          respondWithMediaType(MediaTypes.`application/json`) {
            complete {
              log.info(s"GET events of appId=${appId}")
              val data = eventClient.futureGetByAppId(appId).map { r =>
                r match {
                  case Left(StorageError(message)) =>
                    (StatusCodes.InternalServerError, ("message" -> message))
                  case Right(eventIter) =>
                    if (eventIter.hasNext)
                      (StatusCodes.OK, eventIter.toArray)
                    else
                      (StatusCodes.NotFound, None)
                }
              }
              data
            }
          }
        }
      } ~
      delete {
        parameter('appId.as[Int]) { appId =>
          respondWithMediaType(MediaTypes.`application/json`) {
            complete {
              log.info(s"DELETE events of appId=${appId}")
              val data = eventClient.futureDeleteByAppId(appId).map { r =>
                r match {
                  case Left(StorageError(message)) =>
                    (StatusCodes.InternalServerError, ("message" -> message))
                  case Right(()) =>
                    (StatusCodes.OK, None)
                }
              }
              data
            }
          }
        }
      }
    } ~
    path ("test" / Segment ) { testId =>
      get {
        respondWithMediaType(MediaTypes.`application/json`) {
          complete{
            TestServiceClient.test(TestMessage(testId)).map { m =>
              m match {
                case TestMessage("OK") => (StatusCodes.OK, m)
                case TestMessage("BAD") => (StatusCodes.BadRequest, m)
              }
            }
            /*(testServiceActor ? TestMessage(testId)).mapTo[TestMessage].map(
              x => (StatusCodes.OK, x)
            )*/
          }
        }
      }
    }

  def receive = runRoute(route)

}

object TestServiceClient {

  import scala.concurrent._
  import ExecutionContext.Implicits.global

  def test(msg: TestMessage) = {
    Future {
      msg match {
        case TestMessage("0") => TestMessage("OK")
        case TestMessage("1") => TestMessage("BAD")
      }
    }
  }
}

case class TestMessage(val msg: String)

class TestServiceActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case TestMessage("1") => {
      log.info(s"received 1")
      //sender ! HttpResponse(StatusCodes.BadRequest,
        //entity = "test bad")
      sender ! TestMessage("test bad")
    }
    case TestMessage("0") => {
      log.info("received 0")
      //sender ! HttpResponse(StatusCodes.OK,
      //  entity = "test ok")
      sender ! TestMessage("test ok")// OK
    }
    case _ => sender ! TestMessage("test error")
  }
}



/* message */
case class StartServer(
  val host: String,
  val port: Int
)

class DataServerActor(val eventClient: Events) extends Actor {
  val log = Logging(context.system, this)
  val child = context.actorOf(
    Props(classOf[DataServiceActor], eventClient),
    "DataServiceActor")
  implicit val system = context.system

  def receive = {
    case StartServer(host, portNum) => {
      IO(Http) ! Http.Bind(child, interface = host, port = portNum)
    }
    case m: Http.Bound => log.info("Bound received.")
    case m: Http.CommandFailed => log.error("Command failed.")
    case _ => log.error("Unknown message.")
  }
}


object Run {

  def main (args: Array[String]) {
    implicit val system = ActorSystem("DataAPISystem")

    val storageType = if (args.isEmpty) "ES" else args(0)
    val eventClient = Storage.eventClient(storageType)

    val serverActor = system.actorOf(
      Props(classOf[DataServerActor], eventClient),
      "DataServerActor")
    serverActor ! StartServer("localhost", 8081)

    println("[ Hit any key to exit. ]")
    val result = readLine()
    system.shutdown()
  }

}
