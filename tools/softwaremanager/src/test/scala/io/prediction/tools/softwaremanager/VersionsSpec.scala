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
  "get a non-existent updater"                                                ! latestUpdater()^
  "get version sequence"                                                      ! sequence()^
  "get a version that requires update"                                        ! updateRequired()^
  "get an update sequence"                                                    ! updateSequence()^
                                                                              end

  lazy val versions = Versions(getClass.getResource("/versions.json").getPath)

  def load() = versions must beAnInstanceOf[Versions]

  def latestVersion() = versions.latestVersion must_== "0.5.0"

  def latestBinaries() = versions.binaries(versions.latestVersion) must beSome("http://download.prediction.io/PredictionIO-0.5.0.zip")

  def latestSources() = versions.sources(versions.latestVersion) must beSome("http://download.prediction.io/PredictionIO-0.5.0-sources.zip")

  def latestUpdater() = versions.updater("0.5.1") must beNone

  def sequence() = versions.sequence must_== Seq("0.5.0", "0.5.1", "0.6.0", "0.6.1", "0.6.2", "0.7.0", "0.20.0", "0.20.1", "1.0.0")

  def updateRequired() = versions.updateRequired("0.6.1") must_== true

  def updateSequence() = {
    (versions.updateSequence("0.5.0", "0.20.1") must_== Seq("0.6.1", "0.7.0")) and
      (versions.updateSequence("0.6.0", "1.0.0") must_== Seq("0.6.1", "0.7.0", "0.20.0"))
  }
}
