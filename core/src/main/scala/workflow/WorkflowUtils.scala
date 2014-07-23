package io.prediction.workflow

import io.prediction.controller.Engine
import io.prediction.controller.IEngineFactory
import io.prediction.controller.EmptyParams
import io.prediction.controller.Params
import io.prediction.controller.Utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.language.existentials
import scala.reflect.runtime.universe

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
}

object EngineLanguage extends Enumeration {
  val Scala, Java = Value
}
