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

package io.prediction.workflow

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapterFactory
import io.prediction.controller.Params
import io.prediction.controller.Utils
import io.prediction.workflow.JsonExtractorOption.JsonExtractorOption
import org.json4s.Extraction
import org.json4s.Formats
import org.json4s.JsonAST.JArray
import org.json4s.native.JsonMethods.compact
import org.json4s.native.JsonMethods.parse
import org.json4s.native.JsonMethods.render
import org.json4s.reflect.TypeInfo


object JsonExtractor {

  def toJValue(
    extractorOption: JsonExtractorOption,
    o: Any,
    json4sFormats: Formats = Utils.json4sDefaultFormats,
    gsonTypeAdapterFactories: Seq[TypeAdapterFactory] = Seq.empty[TypeAdapterFactory]) = {

    extractorOption match {
      case JsonExtractorOption.Both =>

          val json4sResult = Extraction.decompose(o)(json4sFormats)
          json4sResult.children.size match {
            case 0 => parse(gson(gsonTypeAdapterFactories).toJson(o))
            case _ => json4sResult
          }
      case JsonExtractorOption.Json4sNative =>
        Extraction.decompose(o)(json4sFormats)
      case JsonExtractorOption.Gson =>
        parse(gson(gsonTypeAdapterFactories).toJson(o))
    }
  }

  def extract[T](
    extractorOption: JsonExtractorOption,
    json: String,
    clazz: Class[T],
    json4sFormats: Formats = Utils.json4sDefaultFormats,
    gsonTypeAdapterFactories: Seq[TypeAdapterFactory] = Seq.empty[TypeAdapterFactory]) = {

    extractorOption match {
      case JsonExtractorOption.Both =>
        try {
          extractWithJson4sNative(json, json4sFormats, clazz)
        } catch {
          case e: Exception =>
            extractWithGson(json, clazz, gsonTypeAdapterFactories)
        }
      case JsonExtractorOption.Json4sNative =>
        extractWithJson4sNative(json, json4sFormats, clazz)
      case JsonExtractorOption.Gson =>
        extractWithGson(json, clazz, gsonTypeAdapterFactories)
    }
  }

  def paramToJson(extractorOption: JsonExtractorOption, param: (String, Params)): String = {
    val toBeReplacedJValue = JsonExtractor.toJValue(extractorOption, (param._1, null))
    val paramJValue = JsonExtractor.toJValue(extractorOption, param._2)

    compact(render(toBeReplacedJValue.replace(param._1 :: Nil, paramJValue)))
  }

  def paramsToJson(extractorOption: JsonExtractorOption, params: Seq[(String, Params)]): String = {
    val jValues = params.map { case (name, param) =>
      val toBeReplacedJValue = JsonExtractor.toJValue(extractorOption, (name, null))
      val paramJValue = JsonExtractor.toJValue(extractorOption, param)

      toBeReplacedJValue.replace(name :: Nil, paramJValue)
    }

    compact(render(JArray(jValues.toList)))
  }

  private def extractWithJson4sNative[T](
    json: String,
    formats: Formats,
    clazz: Class[T]): T = {

    Extraction.extract(parse(json), TypeInfo(clazz, None))(formats).asInstanceOf[T]
  }

  private def extractWithGson[T](
    json: String,
    clazz: Class[T],
    gsonTypeAdapterFactories: Seq[TypeAdapterFactory]): T = {

    gson(gsonTypeAdapterFactories).fromJson(json, clazz)
  }

  private def gson(gsonTypeAdapterFactories: Seq[TypeAdapterFactory]): Gson = {
    val gsonBuilder = new GsonBuilder()
    gsonTypeAdapterFactories.foreach { typeAdapterFactory =>
      gsonBuilder.registerTypeAdapterFactory(typeAdapterFactory)
    }

    gsonBuilder.create()
  }

}
