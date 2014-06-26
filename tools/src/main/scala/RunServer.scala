package io.prediction.tools

import io.prediction.EngineFactory
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseServer
import io.prediction.storage.{ Config, EngineManifest, Run }

import com.twitter.chill.KryoInjection
import com.twitter.chill.ScalaKryoInstantiator
import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization

import java.io.File
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.net.URLClassLoader

import scala.language.existentials
import scala.reflect.runtime.universe

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

class KryoInstantiator(classLoader: ClassLoader) extends ScalaKryoInstantiator {
  override def newKryo = {
    val kryo = super.newKryo
    kryo.setClassLoader(classLoader)
    kryo
  }
}

case class Args(
  id: String = "",
  version: String = "",
  run: String = "",
  ip: String = "localhost",
  port: Int = 8000)

object RunServer extends Logging {
  def getParams[A <: AnyRef](
      formats: Formats,
      jsonString: String,
      classManifest: Manifest[A]): A = {
    val json = parse(jsonString)
    Extraction.extract(json)(formats, classManifest)
  }

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[Args]("RunServer") {
      opt[String]("id") action { (x, c) =>
        c.copy(id = x)
      } text("engine ID")
      opt[String]("version") action { (x, c) =>
        c.copy(version = x)
      } text("engine version")
      opt[String]("ip") action { (x, c) =>
        c.copy(ip = x)
      } text("IP to bind to (default: localhost)")
      opt[Int]("port") action { (x, c) =>
        c.copy(port = x)
      } text("port to bind to (default: 8000)")
      arg[String]("run ID") action { (x, c) =>
        c.copy(run = x)
      }
    }

    parser.parse(args, Args()) map { parsed =>
      val config = new Config
      val runs = config.getSettingsRuns
      val engineManifests = config.getSettingsEngineManifests
      runs.get(parsed.run) map { run =>
        val engineId = if (parsed.id != "") parsed.id else run.engineManifestId
        val engineVersion = if (parsed.version != "") parsed.version else run.engineManifestVersion
        engineManifests.get(engineId, engineVersion) map { manifest =>
          // we need an ActorSystem to host our application in
          implicit val system = ActorSystem("predictionio-server")

          // create and start our service actor
          val service = system.actorOf(Props(classOf[ServerActor], parsed, run, manifest), "server")

          implicit val timeout = Timeout(5.seconds)
          // start a new HTTP server on port 8080 with our service actor as the handler
          IO(Http) ? Http.Bind(service, interface = parsed.ip, port = parsed.port)

        } getOrElse {
          error(s"Invalid Engine ID or version. Aborting server.")
        }
      } getOrElse {
        error(s"Invalid Run ID. Aborting server.")
      }
    }
  }
}

class ServerActor(val args: Args, val run: Run, val manifest: EngineManifest) extends Actor with Server {
  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

case class AlgoParams(name: String, params: JValue)

// this trait defines our service behavior independently from the service actor
trait Server extends HttpService with Logging {
  val args: Args
  val run: Run
  val manifest: EngineManifest

  val engineFactoryName = manifest.engineFactory

  val engineJarFiles = manifest.jars.map(j => new File(j))
  engineJarFiles foreach { f =>
    info(s"Engine JAR file (${f}) exists? ${f.exists}")
  }
  val classLoader = new URLClassLoader(engineJarFiles.map(_.toURI.toURL).toArray)
  val runtimeMirror = universe.runtimeMirror(classLoader)
  val kryoInstantiator = new KryoInstantiator(classLoader)
  val kryo = KryoInjection.instance(kryoInstantiator)

  val engineModule = runtimeMirror.staticModule(engineFactoryName)
  val engineObject = runtimeMirror.reflectModule(engineModule)
  val engine = engineObject.instance.asInstanceOf[EngineFactory]()

  implicit val formats = engine.formats

  val algorithmMap = engine.algorithmClassMap.mapValues(_.newInstance)
  val models = kryo.invert(run.models).map(_.asInstanceOf[Array[Array[Any]]]).get

  val algoJsonSeq = Serialization.read[Seq[AlgoParams]](run.algoParamsList)
  val algoNames = algoJsonSeq.map(_.name)
  val algoParams = algoJsonSeq.map(m => Extraction.extract(m.params)(formats, algorithmMap(m.name).paramsClass))
  algoNames.zipWithIndex map { t =>
    algorithmMap(t._1).initBase(algoParams(t._2))
  }

  val server = engine.serverClass.newInstance
  val serverParams = RunServer.getParams(formats, run.serverParams, server.paramsClass)
  server.initBase(serverParams)

  val firstAlgo = algorithmMap(algoNames(0))
  val featureClass = firstAlgo.featureClass

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <head>
                <title>PredictionIO Server at {args.ip}:{args.port}</title>
              </head>
              <body>
                <h1>PredictionIO Server</h1>
                <div>
                  <ul>
                    <li><strong>URL:</strong> {args.ip}:{args.port}</li>
                    <li><strong>Run ID:</strong> {args.run}</li>
                    <li><strong>Engine:</strong> {manifest.id} {manifest.version}</li>
                    <li><strong>Class:</strong> {engineFactoryName}</li>
                    <li><strong>JARs:</strong> {engineJarFiles.mkString(" ")}</li>
                    <li><strong>Algorithms:</strong> {algorithmMap}</li>
                    <li><strong>Algorithms Parameters:</strong> {algoParams.mkString(" ")}</li>
                    <li><strong>Models:</strong> {models(0).mkString(" ")}</li>
                    <li><strong>Server Parameters:</strong> {serverParams}</li>
                  </ul>
                </div>
              </body>
            </html>
          }
        }
      } ~
      post {
        entity(as[String]) { featureString =>
          val json = parse(featureString)
          val feature = Extraction.extract(json)(formats, featureClass)
          val predictions = algoNames.zipWithIndex map { t =>
            algorithmMap(t._1).predictBase(models(0)(t._2), feature)
          }
          val prediction = server.combineBase(feature, predictions)
          complete(compact(render(Extraction.decompose(prediction))))
        }
      }
    }
}
