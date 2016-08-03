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

package org.apache.predictionio.tools.admin

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestProbe
import org.apache.predictionio.data.storage.Storage
import org.specs2.mutable.Specification
import spray.http._
import spray.httpx.RequestBuilding._
import spray.util._


class AdminAPISpec extends Specification{

  val system = ActorSystem(Utils.actorSystemNameFrom(getClass))
  val config = AdminServerConfig(
    ip = "localhost",
    port = 7071)

  val commandClient = new CommandClient(
    appClient = Storage.getMetaDataApps,
    accessKeyClient = Storage.getMetaDataAccessKeys,
    eventClient = Storage.getLEvents()
  )

  val adminActor= system.actorOf(Props(classOf[AdminServiceActor], commandClient))

  "GET / request" should {
    "properly produce OK HttpResponses" in {
      val probe = TestProbe()(system)
      probe.send(adminActor, Get("/"))

      probe.expectMsg(
        HttpResponse(
          200,
          HttpEntity(
            contentType = ContentTypes.`application/json`,
            string = """{"status":"alive"}"""
          )
        )
      )
      success
    }
  }

  "GET /cmd/app request" should {
    "properly produce OK HttpResponses" in {
      /*
      val probe = TestProbe()(system)
      probe.send(adminActor,Get("/cmd/app"))

      //TODO: Need to convert the response string to the corresponding case object to assert some properties on the object
      probe.expectMsg(
        HttpResponse(
          200,
          HttpEntity(
            contentType = ContentTypes.`application/json`,
            string = """{"status":1}"""
          )
        )
      )*/
      pending
    }
  }

  step(system.shutdown())
}
