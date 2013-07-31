package io.prediction.tools.softwaremanager

import org.specs2._
import org.specs2.specification.Step

class VersionsSpec extends Specification { def is =
  "PredictionIO Software Manager Versions Specification"                      ^
                                                                              p^
  "load version file"                                                         ! load()^
  "get latest version"                                                        ! latestVersion()^
  "get latest version binaries"                                               ! latestBinaries()^
  "get latest version sources"                                                ! latestSources()^
  "get latest version updater"                                                ! latestUpdater()^
                                                                              end

  lazy val versions = Versions(getClass.getResource("/versions.json").getPath)

  def load() = versions must beAnInstanceOf[Versions]

  def latestVersion() = versions.latestVersion must_== "0.5.0"

  def latestBinaries() = versions.binaries(versions.latestVersion) must beSome("http://download.prediction.io/PredictionIO-0.5.0.zip")

  def latestSources() = versions.sources(versions.latestVersion) must beSome("http://download.prediction.io/PredictionIO-0.5.0-sources.zip")

  def latestUpdater() = versions.updater(versions.latestVersion) must beNone
}
