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

package io.prediction.data.storage.jdbc

import grizzled.slf4j.Logging
import io.prediction.data.storage.BaseStorageClient
import io.prediction.data.storage.StorageClientConfig
import io.prediction.data.storage.StorageClientException
import scalikejdbc._

class StorageClient(val config: StorageClientConfig)
  extends BaseStorageClient with Logging {
  override val prefix = "JDBC"

  if (!config.properties.contains("URL"))
    throw new StorageClientException("The URL variable is not set!")
  if (!config.properties.contains("USERNAME"))
    throw new StorageClientException("The USERNAME variable is not set!")
  if (!config.properties.contains("PASSWORD"))
    throw new StorageClientException("The PASSWORD variable is not set!")

  ConnectionPool.singleton(
    config.properties("URL"),
    config.properties("USERNAME"),
    config.properties("PASSWORD"))
  val client = config.properties("URL")
}
