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

package org.apache.predictionio.data.storage

import org.scalamock.specs2.MockContext

trait StorageMockContext extends MockContext {

  if(!EnvironmentFactory.environmentService.isDefined){
    val mockedEnvService = mock[EnvironmentService]
    (mockedEnvService.envKeys _)
      .expects
      .returning(List("PIO_STORAGE_REPOSITORIES_METADATA_NAME",
        "PIO_STORAGE_SOURCES_MYSQL_TYPE",
        "PIO_STORAGE_REPOSITORIES_EVENTDATA_NAME",
        "PIO_STORAGE_SOURCES_EVENTDATA_TYPE"))
      .twice

    (mockedEnvService.getByKey _)
      .expects("PIO_STORAGE_REPOSITORIES_METADATA_NAME")
      .returning("test_metadata")

    (mockedEnvService.getByKey _)
      .expects("PIO_STORAGE_REPOSITORIES_METADATA_SOURCE")
      .returning("MYSQL")

    (mockedEnvService.getByKey _)
      .expects("PIO_STORAGE_REPOSITORIES_EVENTDATA_NAME")
      .returning("test_eventdata")

    (mockedEnvService.getByKey _)
      .expects("PIO_STORAGE_REPOSITORIES_EVENTDATA_SOURCE")
      .returning("MYSQL")

    (mockedEnvService.getByKey _)
      .expects("PIO_STORAGE_SOURCES_MYSQL_TYPE")
      .returning("jdbc")

    (mockedEnvService.filter _)
      .expects(*)
      .returning(Map(
        "URL" -> "jdbc:h2:~/test;MODE=MySQL;AUTO_SERVER=TRUE",
        "USERNAME" -> "sa",
        "PASSWORD" -> "")
      )

    EnvironmentFactory.environmentService = new Some(mockedEnvService)
  }
}
