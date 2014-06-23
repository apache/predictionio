package io.prediction.tools

import io.prediction.BaseModel
import io.prediction.{ EngineFactory, EvaluatorFactory }
import io.prediction.storage.{ Config, EngineManifest, Run }

import com.twitter.chill.KryoInjection
import com.twitter.chill.ScalaKryoInstantiator
import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import org.json4s.jackson.JsonMethods._

import java.io.File
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.net.URLClassLoader

import scala.language.existentials
import scala.reflect.runtime.universe

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
          val evaluatorFactoryName = manifest.evaluatorFactory
          val engineFactoryName = manifest.engineFactory

          val engineJarFile = new File(manifest.jar)
          info(s"Engine JAR file (${manifest.jar}) exists? ${engineJarFile.exists()}")
          val classLoader = new URLClassLoader(Array(engineJarFile.toURI.toURL))
          val runtimeMirror = universe.runtimeMirror(classLoader)
          val kryoInstantiator = new KryoInstantiator(classLoader)
          val kryo = KryoInjection.instance(kryoInstantiator)

          val engineModule = runtimeMirror.staticModule(engineFactoryName)
          val engineObject = runtimeMirror.reflectModule(engineModule)
          val engine = engineObject.instance.asInstanceOf[EngineFactory]()

          val algorithmMap = engine.algorithmClassMap.mapValues(_.newInstance)
          val models = kryo.invert(run.models).map(_.asInstanceOf[Array[Array[BaseModel]]])

          val server = engine.serverClass.newInstance
          val serverParams = getParams(run.serverParams, server.paramsClass)
          server.initBase(serverParams)

          debug(algorithmMap)
          models.get foreach { e =>
            e foreach {
              debug(_)
            }
          }
          debug(server)
          debug(serverParams)
        } getOrElse {
          error(s"Invalid Engine ID or version. Aborting server.")
        }
      } getOrElse {
        error(s"Invalid Run ID. Aborting server.")
      }
    }
  }
}
