package io.prediction.dataapi.api

import io.prediction.dataapi.storage.Events
import io.prediction.dataapi.storage.Event
import io.prediction.dataapi.storage.StorageError
import io.prediction.dataapi.storage.Storage
import io.prediction.dataapi.storage.EventJson4sSupport

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.io.IO
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{ read, write }
//import org.json4s.ext.JodaTimeSerializers

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import spray.http.StatusCodes
import spray.http.MediaTypes
import spray.http.HttpCharsets
import spray.http.HttpEntity
import spray.http.HttpResponse
import spray.httpx.Json4sSupport
import spray.httpx.unmarshalling.Unmarshaller
import spray.can.Http
import spray.routing._
import Directives._

class DataServiceActor(val eventClient: Events) extends HttpServiceActor {

  object Json4sProtocol extends Json4sSupport {
    implicit def json4sFormats: Formats = DefaultFormats +
      new EventJson4sSupport.APISerializer
    //implicit def json4sFormats: Formats = DefaultFormats.lossless ++
    //  JodaTimeSerializers.all
  }

  import Json4sProtocol._

  val log = Logging(context.system, this)

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our
  // Futures
  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  private def stringToDateTime(dt: String): DateTime =
    ISODateTimeFormat.dateTimeParser.parseDateTime(dt)

  val testServiceActor = actorRefFactory.actorOf(Props[TestServiceActor],
    "TestServiceActor")

  val rejectionHandler = RejectionHandler {
    case MalformedRequestContentRejection(msg, _) :: _ =>
      complete(StatusCodes.BadRequest, ("message" -> msg))
  }

  implicit val TestMessageUnmarshaller =
    Unmarshaller[TestMessage](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty => {
        val json = read[JValue](
          x.asString(defaultCharset = HttpCharsets.`UTF-8`))
        val msg = (json \ "msg").extract[String]
        TestMessage(msg)
      }
    }

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
        parameters('appId.as[Int], 'startTime.as[Option[String]],
          'untilTime.as[Option[String]]) {
          (appId, startTimeStr, untilTimeStr) =>
          respondWithMediaType(MediaTypes.`application/json`) {
            complete {
              log.info(
                s"GET events of appId=${appId} ${startTimeStr} ${untilTimeStr}")

              // TODO: handle parse datetime error
              val startTime = startTimeStr.map(stringToDateTime(_))
              val untilTime = untilTimeStr.map(stringToDateTime(_))

              val data = if ((startTime != None) || (untilTime != None)) {
                eventClient.futureGetByAppIdAndTime(appId,
                  startTime, untilTime).map { r =>
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
              } else {
                eventClient.futureGetByAppId(appId).map { r =>
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
    } ~
    path ("test") {
      post {
        respondWithMediaType(MediaTypes.`application/json`) {
          handleRejections(rejectionHandler) {
            entity(as[TestMessage]) { obj =>
              //val map = obj.obj.toMap
              complete {
                log.info(s"receve ${obj}")
                (StatusCodes.OK, None)
              }

            }
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

case class TestMessage(val msg: String) {
  require(!msg.isEmpty, "Can't be empty")
}

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
