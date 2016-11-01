/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.predictionio.tools.commands

import org.apache.predictionio.tools.Runner
import org.apache.predictionio.tools.SparkArgs
import org.apache.predictionio.tools.ReturnTypes._

import scala.sys.process._

case class ImportArgs(
  appId: Int = 0,
  channel: Option[String] = None,
  inputPath: String = "")

object Import {
  def fileToEvents(
    ia: ImportArgs,
    sa: SparkArgs,
    pioHome: String): Expected[(Process, () => Unit)] = {

    val channelArg = ia.channel
      .map(ch => Seq("--channel", ch)).getOrElse(Nil)
    Runner.runOnSpark(
      "org.apache.predictionio.tools.imprt.FileToEvents",
      Seq(
        "--appid",
        ia.appId.toString,
        "--input",
        ia.inputPath) ++ channelArg,
      sa,
      Nil,
      pioHome)
  }
}
