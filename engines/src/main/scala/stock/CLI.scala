package io.prediction.engines.stock

import io.prediction.core.BaseEngine

import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import com.github.nscala_time.time.Imports._
import org.json4s.DefaultFormats  

object CLI {
  def main(args: Array[String]) {
    val engine = StockEngineFactory.get

    val json = parse("""{ "i" : "X" }""")

    val server = engine.serverClass.newInstance

    
    val json2 = parse("""{ "i" : 1699 }""")
    
    println(json)
    println(Extraction.extract(json)(DefaultFormats, server.paramsClass))
    println
    println(json2)
    println(Extraction.extract(json2)(DefaultFormats, server.paramsClass))
  }
}


