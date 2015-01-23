/** Copyright 2014 TappingStone, Inc.
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

package io.prediction.data.storage

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext

case class AppNewRequest()
case class AppNewResponse()

case class AppListResponse()

// see Console.scala for reference

class CommandClient (
  val appClient: Apps,
  val accessKeyClient: AccessKeys,
  val eventClient: LEvents
) {

  // see def appNew() in Console.scala
  def futureAppNew(req: AppNewRequest)
    (implicit ec: ExecutionContext): Future[AppNewResponse] = Future {
    AppNewResponse()
  }

  // see def appList() in Console.scala
  def futureAppList()
    (implicit ec: ExecutionContext): Future[AppListResponse] = Future {
    AppListResponse()
  }

  //def futureAppDelete() = ...

  //def futureAppDataDelete() = ...

}
