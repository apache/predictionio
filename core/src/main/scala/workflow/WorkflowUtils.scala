package io.prediction.workflow

import io.prediction.controller.Engine
import io.prediction.controller.IEngineFactory
import io.prediction.controller.IPersistentModelLoader
import io.prediction.controller.EmptyParams
import io.prediction.controller.Params
import io.prediction.controller.Utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import grizzled.slf4j.Logging
import org.apache.spark.SparkContext
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.language.existentials
import scala.reflect._
import scala.reflect.runtime.universe

import scala.io.Source
import java.io.FileNotFoundException
import java.util.concurrent.Callable
import java.lang.Thread

/** Collection of reusable workflow related utilities. */
object WorkflowUtils extends Logging {
  @transient private lazy val gson = new Gson

  /** Obtains an Engine object in Scala, or instantiate an Engine in Java.
    *
    * @param engine Engine factory name.
    * @param cl A Java ClassLoader to look for engine-related classes.
    *
    * @throws ClassNotFoundException
    *         Thrown when engine factory class does not exist.
    * @throws NoSuchMethodException
    *         Thrown when engine factory's apply() method is not implemented.
    */
  def getEngine(engine: String, cl: ClassLoader) = {
    val runtimeMirror = universe.runtimeMirror(cl)
    val engineModule = runtimeMirror.staticModule(engine)
    val engineObject = runtimeMirror.reflectModule(engineModule)
    try {
      (
        EngineLanguage.Scala,
        engineObject.instance.asInstanceOf[IEngineFactory]()
      )
    } catch {
      case e @ (_: NoSuchFieldException | _: ClassNotFoundException) => try {
        (
          EngineLanguage.Java,
          Class.forName(engine).newInstance.asInstanceOf[IEngineFactory]()
        )
      }
    }
  }

  def getPersistentModel[AP <: Params, M](
      pmm: PersistentModelManifest,
      runId: String,
      params: AP,
      sc: Option[SparkContext],
      cl: ClassLoader): M = {
    val runtimeMirror = universe.runtimeMirror(cl)
    val pmmModule = runtimeMirror.staticModule(pmm.className)
    val pmmObject = runtimeMirror.reflectModule(pmmModule)
    try {
      pmmObject.instance.asInstanceOf[IPersistentModelLoader[AP, M]](
        runId,
        params,
        sc)
    } catch {
      case e @ (_: NoSuchFieldException | _: ClassNotFoundException) => try {
        val loadMethod = Class.forName(pmm.className).getMethod(
          "load",
          classOf[String],
          classOf[Params],
          classOf[SparkContext])
        loadMethod.invoke(null, runId, params, sc.getOrElse(null)).asInstanceOf[M]
      } catch {
        case e: ClassNotFoundException =>
          error(s"Model class ${pmm.className} cannot be found.")
          throw e
        case e: NoSuchMethodException =>
          error(
            "The load(String, Params, SparkContext) method cannot be found.")
          throw e
      }
    }
  }

  /** Converts a JSON document to an instance of Params.
    *
    * @param language Engine's programming language.
    * @param json JSON document.
    * @param clazz Class of the component that is going to receive the resulting
    *              Params instance as a constructor argument.
    * @param formats JSON4S serializers for deserialization.
    *
    * @throws MappingException Thrown when JSON4S fails to perform conversion.
    * @throws JsonSyntaxException Thrown when GSON fails to perform conversion.
    */
  def extractParams(
      language: EngineLanguage.Value = EngineLanguage.Scala,
      json: String,
      clazz: Class[_],
      formats: Formats = Utils.json4sDefaultFormats): Params = {
    implicit val f = formats
    val pClass = clazz.getConstructors.head.getParameterTypes
    if (pClass.size == 0) {
      if (json != "")
        warn(s"Non-empty parameters supplied to ${clazz.getName}, but its " +
          "constructor does not accept any arguments. Stubbing with empty " +
          "parameters.")
      EmptyParams()
    } else {
      val apClass = pClass.head
      language match {
        case EngineLanguage.Java => try {
          gson.fromJson(json, apClass)
        } catch {
          case e: JsonSyntaxException =>
            error(s"Unable to extract parameters for ${apClass.getName} from " +
              s"JSON string: ${json}. Aborting workflow.")
            throw e
        }
        case EngineLanguage.Scala => try {
          Extraction.extract(parse(json), reflect.TypeInfo(apClass, None)).
            asInstanceOf[Params]
        } catch {
          case me: MappingException => {
            error(s"Unable to extract parameters for ${apClass.getName} from " +
              s"JSON string: ${json}. Aborting workflow.")
            throw me
          }
        }
      }
    }
  }

  /** Converts Java (non-Scala) objects to a JSON4S JValue.
    *
    * @param params The Java object to be converted.
    */
  def javaObjectToJValue(params: AnyRef): JValue = parse(gson.toJson(params))

  private [prediction] def checkUpgrade(component: String = "core"): Unit = {
    val runner = new Thread(new UpgradeCheckRunner(component))
    runner.start
  }
}

class UpgradeCheckRunner(val component: String) extends Runnable with Logging {
  val version = "0.8.0-SNAPSHOT"
  val versionsHost = "http://direct.prediction.io/"

  def run(): Unit = {
    val url = s"${versionsHost}${version}/${component}.json"
    try {
      val upgradeData = Source.fromURL(url)
    } catch {
      case e: FileNotFoundException => {
        warn("Update metainfo not found. $url")
      }
    }
    // TODO: Implement upgrade logic
  }
}


object EngineLanguage extends Enumeration {
  val Scala, Java = Value
}
