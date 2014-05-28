package io.prediction.engines.stock

import io.prediction.core.BaseEngine

import grizzled.slf4j.Logging
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
//import org.json4s.ext.JodaTimeSerializers

import com.github.nscala_time.time.Imports._

object CLI {
  def main(args: Array[String]) {
    val engine = StockEngineFactory.get

    val json = parse("""{ "i" : "X" }""")

    val j = engine.serverClass.newInstance.json2Params(json)
    println(json)
    println(j)
    println
    
    val json2 = parse("""{ "i" : 1699 }""")

    val i = engine.serverClass.newInstance.json2Params(json2)
    println(json2)
    println(i)
  
  }
}


