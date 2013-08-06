package io.prediction.tools.softwaremanager

import scala.reflect.ClassTag
import scala.util.parsing.json.JSON

/** Extractors: http://stackoverflow.com/questions/4170949/how-to-parse-json-in-scala-using-standard-scala-classes */
class CC[T : ClassTag] { def unapply(a: Any)(implicit e: ClassTag[T]): Option[T] = {
  try { Some(e.runtimeClass.cast(a).asInstanceOf[T]) } catch { case _: Throwable => None } }
}

object M extends CC[Map[String, Any]]
object MSS extends CC[Map[String, String]]
object SS extends CC[Seq[String]]
object OSS extends CC[Option[Seq[String]]]
object S extends CC[String]
object OS extends CC[Option[String]]

class JSONParseException(message: String) extends Exception(message)
class VersionsFormatException(message: String) extends Exception(message)

class Versions(localVersion: Option[String]) {
  private val versionsString = localVersion map { lv =>
    scala.io.Source.fromFile(lv).mkString
  } getOrElse {
    scala.io.Source.fromURL(Versions.versionsUrl).mkString
  }

  private val versionsJson = JSON.parseFull(versionsString) getOrElse { throw new JSONParseException("Invalid JSON file.") }

  private val meta = M.unapply(versionsJson) getOrElse { throw new JSONParseException("Root level is not a valid JSON object.") }

  val latestVersion = S.unapply(meta("latest")) getOrElse { throw new VersionsFormatException("Cannot find the latest version.") }

  private val versions = M.unapply(meta("versions")) getOrElse { throw new VersionsFormatException("Cannot find versions information.") }

  private def versionToParts(version: String): Seq[Int] = version.split('.').map(_.toInt)

  private def versionLessThan(x: String, y: String): Boolean = {
    val versionX = versionToParts(x)
    val versionY = versionToParts(y)

    if (versionX(0) == versionY(0))
      if (versionX(1) == versionY(1))
        versionX(2) < versionY(2)
      else
        versionX(1) < versionY(1)
    else
      versionX(0) < versionY(0)
  }

  val sequence = versions.keys.toSeq.sortWith { (x, y) => versionLessThan(x, y) }

  def version(version: String): Option[Map[String, String]] = versions.get(version) map { MSS.unapply(_) map { Some(_) } getOrElse None } getOrElse None

  def binaries(ver: String): Option[String] = version(ver) map { _.get("binaries") map { Some(_) } getOrElse None } getOrElse None

  def sources(ver: String): Option[String] = version(ver) map { _.get("sources") map { Some(_) } getOrElse None } getOrElse None

  def updater(ver: String): Option[String] = version(ver) map { _.get("updater") map { Some(_) } getOrElse None } getOrElse None

  def updateRequired(ver: String): Boolean = updater(ver) map { _ => true } getOrElse false

  def updateSequence(fromVersion: String, toVersion: String): Seq[String] = {
    val seqWithAllUpdates = sequence filter { updateRequired(_) }
    val seqWithUpdatesUntilTo = seqWithAllUpdates filter { v =>
      versionLessThan(v, toVersion) || v == toVersion
    }
    val seqWithUpdates = seqWithUpdatesUntilTo filterNot { v =>
      versionLessThan(v, fromVersion) || v == fromVersion
    }
    seqWithUpdates.dropRight(1)
  }
}

object Versions {
  def apply() = {
    new Versions(None)
  }

  def apply(localVersion: String) = {
    new Versions(Some(localVersion))
  }

  val versionsUrl = "http://direct.prediction.io/versions.json"
}
