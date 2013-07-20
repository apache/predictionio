package io.prediction.commons.settings

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._

class SystemInfosSpec extends Specification { def is =
  "PredictionIO SystemInfos Specification"                                    ^
                                                                              p^
  "SystemInfos can be implemented by:"                                        ^ endp^
    "1. MongoSystemInfos"                                                     ^ mongoSystemInfos^end

  def mongoSystemInfos =                                                      p^
    "MongoSystemInfos should"                                                 ^
      "behave like any SystemInfos implementation"                            ^ systemInfos(newMongoSystemInfos)^
                                                                              Step(MongoConnection()(mongoDbName).dropDatabase())

  def systemInfos(systemInfos: SystemInfos) = {                               t^
    "create and get a system info entry"                                      ! insertAndGet(systemInfos)^
    "update a system info entry"                                              ! update(systemInfos)^
    "delete a system info entry"                                              ! delete(systemInfos)^
    "backup and restore system info entries"                                  ! backuprestore(systemInfos)^
                                                                              bt
  }

  val mongoDbName = "predictionio_mongosysteminfos_test"
  def newMongoSystemInfos = new mongodb.MongoSystemInfos(MongoConnection()(mongoDbName))

  def insertAndGet(systemInfos: SystemInfos) = {
    val version = SystemInfo(
      id = "version",
      value = "0.4-SNAPSHOT",
      description = Some("PredictionIO Version"))
    systemInfos.insert(version)
    systemInfos.get("version") must beSome(version)
  }

  def update(systemInfos: SystemInfos) = {
    val build = SystemInfo(
      id = "build",
      value = "123",
      description = None)
    systemInfos.insert(build)
    val updatedBuild = build.copy(value = "124")
    systemInfos.update(updatedBuild)
    systemInfos.get("build") must beSome(updatedBuild)
  }

  def delete(systemInfos: SystemInfos) = {
    val foo = SystemInfo(
      id = "foo",
      value = "bar",
      description = None)
    systemInfos.insert(foo)
    systemInfos.delete("foo")
    systemInfos.get("foo") must beNone
  }

  def backuprestore(systemInfos: SystemInfos) = {
    val rev = SystemInfo(
      id = "rev",
      value = "321",
      description = Some("software revision"))
    systemInfos.insert(rev)
    val fn = "systeminfos.bin"
    val fos = new java.io.FileOutputStream(fn)
    try {
      fos.write(systemInfos.backup())
    } finally {
      fos.close()
    }
    systemInfos.restore(scala.io.Source.fromFile(fn)(scala.io.Codec.ISO8859).map(_.toByte).toArray) map { data =>
      data must contain(rev)
    } getOrElse 1 === 2
  }
}
