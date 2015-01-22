package io.prediction.tools.console

import io.prediction.controller.Utils
import io.prediction.tools.ConsoleArgs

import grizzled.slf4j.Logging
import org.apache.commons.io.FileUtils
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{ read, write }
import scalaj.http._

import scala.io.Source
import scala.sys.process._

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

case class NewArgs(
  directory: String = "",
  repository: String = "",
  name: Option[String] = None,
  packageName: Option[String] = None,
  email: Option[String] = None,
  indexUrl: String = "https://engines.prediction.io/engines.json")

case class GitHubTag(
  name: String,
  zipball_url: String,
  tarball_url: String,
  commit: GitHubCommit)

case class GitHubCommit(
  sha: String,
  url: String)

case class GitHubCache(
  headers: Map[String, String],
  body: String)

object Template extends Logging {
  implicit val formats = Utils.json4sDefaultFormats

  def getGitHubRepos(
      repos: Seq[String],
      apiType: String,
      repoFilename: String): Map[String, GitHubCache] = {
    val reposCache = try {
      val cache =
        Source.fromFile(repoFilename)(scala.io.Codec.ISO8859).mkString
        read[Map[String, GitHubCache]](cache)
    } catch {
      case e: Throwable => Map[String, GitHubCache]()
    }
    val newReposCache = repos.map { repo =>
      val url = s"https://api.github.com/repos/${repo}/${apiType}"
      val http = Http(url)
      val response = reposCache.get(repo).map { cache =>
        cache.headers.get("ETag").map { etag =>
          http.header("If-None-Match", etag).asString
        } getOrElse {
          http.asString
        }
      } getOrElse {
        http.asString
      }

      val body = if (response.code == 304) {
        reposCache(repo).body
      } else {
        response.body
      }

      (repo -> GitHubCache(headers = response.headers, body = body))
    }.toMap
    FileUtils.writeStringToFile(
      new File(repoFilename),
      write(newReposCache),
      "ISO-8859-1")
    newReposCache
  }

  def sub(repo: String, name: String, email: String, org: String) = {
    val data = Map(
      "repo" -> repo,
      "name" -> name,
      "email" -> email,
      "org" -> org)
    try {
      scalaj.http.Http("http://update.prediction.io/templates.subscribe").
        postData("json=" + write(data)).asString
    } catch {
      case e: Throwable => error("Unable to subscribe.")
    }
  }

  def meta(repo: String, name: String, org: String) = {
    val data = Map(
      "repo" -> repo,
      "name" -> name,
      "org" -> org)
    try {
      scalaj.http.Http(
        s"http://templates.prediction.io/${repo}/${org}/${name}").asString
    } catch {
      case e: Throwable => warn("Template metadata unavailable.")
    }
  }

  def get(ca: ConsoleArgs): Int = {
    val repoNames = Seq(ca.newArgs.repository)
    val repos = getGitHubRepos(repoNames, "tags", ".templates-cache")

    if (repos.isEmpty) {
      error(s"Failed to retrieve ${repoNames.head}. Aborting.")
      return 1
    }

    val name = ca.newArgs.name getOrElse {
      try {
        Process("git config --global user.name").lines.toList(0)
      } catch {
        case e: java.io.IOException =>
          readLine("Please enter author's name: ")
      }
    }

    val organization = ca.newArgs.packageName getOrElse {
      readLine(
        "Please enter the template's Scala package name (e.g. com.mycompany): ")
    }

    val email = ca.newArgs.email getOrElse {
      try {
        Process("git config --global user.email").lines.toList(0)
      } catch {
        case e: java.io.IOException =>
          readLine("Please enter author's e-mail address: ")
      }
    }

    println(s"Author's name:         ${name}")
    println(s"Author's e-mail:       ${email}")
    println(s"Author's organization: ${organization}")

    var subscribe = readLine("Would you like to be informed about new bug " +
      "fixes and security updates of this template? (Y/n) ")
    var valid = false

    do {
      subscribe match {
        case "" | "Y" | "y" =>
          sub(ca.newArgs.repository, name, email, organization)
          valid = true
        case "n" | "N" =>
          meta(ca.newArgs.repository, name, organization)
          valid = true
        case _ =>
          println("Please answer 'y' or 'n'")
          subscribe = readLine("(Y/n)? ")
      }
    } while (!valid)

    repos.map { repo =>
      println(s"Retrieving ${repo._1}")
      val tags = read[List[GitHubTag]](repo._2.body)
      println(s"There are ${tags.size} tags")
      tags.headOption.map { tag =>
        println("Using the most recent tag")
        val url = s"https://github.com/${repo._1}/archive/${tag.name}.zip"
        println(s"Going to download ${url}")
        val trial = Http(url).asBytes
        val finalTrial = trial.location.map { loc =>
          println(s"Redirecting to ${loc}")
          Http(loc).asBytes
        } getOrElse trial
        val zipFilename = s"${repo._1.replace('/', '-')}-${tag.name}.zip"
        FileUtils.writeByteArrayToFile(
          new File(zipFilename),
          finalTrial.body)
        val zis = new ZipInputStream(
          new BufferedInputStream(new FileInputStream(zipFilename)))
        val bufferSize = 4096
        var ze = zis.getNextEntry
        while (ze != null) {
          val filenameSegments = ze.getName.split(File.separatorChar)
          val destFilename = (ca.newArgs.directory +: filenameSegments.tail).
            mkString(File.separator)
          if (ze.isDirectory) {
            new File(destFilename).mkdirs
          } else {
            val os = new BufferedOutputStream(
              new FileOutputStream(destFilename),
              bufferSize)
            var data = Array.ofDim[Byte](bufferSize)
            var count = zis.read(data, 0, bufferSize)
            while (count != -1) {
              os.write(data, 0, count)
              count = zis.read(data, 0, bufferSize)
            }
            os.flush
            os.close

            val nameOnly = new File(destFilename).getName

            if (organization != "" &&
              (nameOnly.endsWith(".scala") || nameOnly == "build.sbt")) {
              val scalaContent = Source.fromFile(destFilename).getLines
              val processedLines = scalaContent.map { l =>
                val segments = l.split(' ').filterNot(_ == "")
                if (l.startsWith("package")) {
                  s"package ${organization}"
                } else if (nameOnly == "build.sbt" &&
                  segments.size > 2 &&
                  segments.contains("organization") &&
                  segments.indexOf("organization") < segments.indexOf(":=")) {
                  val i = segments.indexOf(":=") + 1
                  val quotedOrg = s""""$organization""""
                  if (segments.size == i + 1)
                    (segments.slice(0, i) :+ quotedOrg).mkString(" ")
                  else
                    ((segments.slice(0, i) :+ quotedOrg) ++
                      segments.slice(i + 1, segments.size)).mkString(" ")
                } else l
              }
              FileUtils.writeStringToFile(
                new File(destFilename),
                processedLines.mkString("\n"))
            }
          }
          ze = zis.getNextEntry
        }
        zis.close
      }
    }

    0
  }
}
