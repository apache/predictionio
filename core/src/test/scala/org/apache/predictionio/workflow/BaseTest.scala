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

// package org.apache.spark
package org.apache.predictionio.workflow

import _root_.io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}
import org.apache.predictionio.data.storage.{EnvironmentFactory, EnvironmentService}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.scalamock.scalatest.MockFactory


/** Manages a local `sc` {@link SparkContext} variable, correctly stopping it
  * after each test. */
trait LocalSparkContext
extends BeforeAndAfterEach with BeforeAndAfterAll { self: Suite =>

  @transient var sc: SparkContext = _

  override def beforeAll() {
    InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory())
    super.beforeAll()
  }

  override def afterEach() {
    resetSparkContext()
    super.afterEach()
  }

  def resetSparkContext() : Unit = {
    LocalSparkContext.stop(sc)
    sc = null
  }

}

object LocalSparkContext {
  def stop(sc: SparkContext) {
    if (sc != null) {
      sc.stop()
    }
    // To avoid Akka rebinding to the same port, since it doesn't unbind immediately on shutdown
    System.clearProperty("spark.driver.port")
  }

  /** Runs `f` by passing in `sc` and ensures that `sc` is stopped. */
  def withSpark[T](sc: SparkContext)(f: SparkContext => T) : Unit = {
    try {
      f(sc)
    } finally {
      stop(sc)
    }
  }

}
/** Shares a local `SparkContext` between all tests in a suite and closes it at the end */
trait SharedSparkContext extends BeforeAndAfterAll { self: Suite =>

  @transient private var _sc: SparkContext = _

  def sc: SparkContext = _sc

  var conf = new SparkConf(false)

  override def beforeAll() {
    _sc = new SparkContext("local[4]", "test", conf)
    super.beforeAll()
  }

  override def afterAll() {
    LocalSparkContext.stop(_sc)
    _sc = null
    super.afterAll()
  }
}

trait SharedStorageContext extends BeforeAndAfterAll { self: Suite =>

  override def beforeAll(): Unit ={
    ConfigurationMockUtil.createJDBCMockedConfig
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

}

object ConfigurationMockUtil extends MockFactory {

  def createJDBCMockedConfig: Unit = {
    val mockedEnvService = mock[EnvironmentService]
    (mockedEnvService.envKeys _)
      .expects
      .returning(List("PIO_STORAGE_REPOSITORIES_METADATA_NAME",
        "PIO_STORAGE_SOURCES_MYSQL_TYPE"))
      .twice

    (mockedEnvService.getByKey _)
      .expects("PIO_STORAGE_REPOSITORIES_METADATA_NAME")
      .returning("test_metadata")

    (mockedEnvService.getByKey _)
      .expects("PIO_STORAGE_REPOSITORIES_METADATA_SOURCE")
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

