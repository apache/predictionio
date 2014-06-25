package io.prediction.tools

import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseServer
import io.prediction.{ EngineFactory, EvaluatorFactory }
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

object RunServer extends Logging {
  case class Args(
    id: String = "",
    version: String = "",
    run: String = "")

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  } ++ JodaTimeSerializers.all

  def getParams[A <: AnyRef](
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
          implicit val system = ActorSystem("on-spray-can")

          // create and start our service actor
          val service = system.actorOf(Props(classOf[MyServiceActor], run, manifest), "demo-service")

          implicit val timeout = Timeout(5.seconds)
          // start a new HTTP server on port 8080 with our service actor as the handler
          IO(Http) ? Http.Bind(service, interface = "localhost", port = 8000)

        } getOrElse {
          error(s"Invalid Engine ID or version. Aborting server.")
        }
      } getOrElse {
        error(s"Invalid Run ID. Aborting server.")
      }
    }
  }
}

class MyServiceActor(val run: Run, val manifest: EngineManifest) extends Actor with MyService {
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
trait MyService extends HttpService with Logging {
  val run: Run
  val manifest: EngineManifest

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.SSSX")
  } ++ JodaTimeSerializers.all

  val evaluatorFactoryName = manifest.evaluatorFactory
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

  val algorithmMap = engine.algorithmClassMap.mapValues(_.newInstance)
  val models = kryo.invert(run.models).map(_.asInstanceOf[Array[Array[Any]]]).get

  debug(run.algoParamsList)
  val algoJsonSeq = Serialization.read[Seq[AlgoParams]](run.algoParamsList)
  debug(algoJsonSeq)
  val algoNames = algoJsonSeq.map(_.name)
  debug(algoNames)
  val algoParams = algoJsonSeq.map(m => Extraction.extract(m.params)(formats, algorithmMap(m.name).paramsClass))
  algoNames.zipWithIndex map { t =>
    algorithmMap(t._1).initBase(algoParams(t._2))
  }
  debug(algoParams)

  val server = engine.serverClass.newInstance
  val serverParams = RunServer.getParams(run.serverParams, server.paramsClass)
  server.initBase(serverParams)

  val firstAlgo = algorithmMap(algoNames(0))
  val featureClass = firstAlgo.featureClass
  debug(featureClass)

  debug(algorithmMap)
  models foreach { m =>
    m foreach {
      debug(_)
    }
  }
  debug(server)
  debug(serverParams)

  debug(models)

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <body>
                <h1>Say hello to <i>spray-routing</i> on <i>spray-can</i>!</h1>
              </body>
            </html>
          }
        }
      }
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
