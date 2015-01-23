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

case class TemplateArgs(
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

case class TemplateEntry(
  repo: String)

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
    val newReposCache = reposCache ++ (repos.map { repo =>
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
    }.toMap)
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

  def list(ca: ConsoleArgs): Int = {
    val templatesUrl = "http://templates.prediction.io/index.json"
    try {
      val templatesJson = Source.fromURL(templatesUrl).mkString("")
      val templates = read[List[TemplateEntry]](templatesJson)
      println("The following is a list of template IDs officially recognized " +
        "by PredictionIO:")
      println()
      templates.foreach { template =>
        println(template.repo)
      }
      println()
      println("Notice that it is possible use any GitHub repository as your " +
        "engine template ID (e.g. YourOrg/YourTemplate).")
      0
    } catch {
      case e: Throwable =>
        error(s"Unable to list templates from ${templatesUrl} " +
          s"(${e.getMessage}). Aborting.")
        1
    }
  }

  def get(ca: ConsoleArgs): Int = {
    val repos =
      getGitHubRepos(Seq(ca.template.repository), "tags", ".templates-cache")

    repos.get(ca.template.repository).map { repo =>
      try {
        read[List[GitHubTag]](repo.body)
      } catch {
        case e: MappingException =>
          error(s"Either ${ca.template.repository} is not a valid GitHub repository, or it does not have any tag. Aborting.")
          return 1
      }
    } getOrElse {
      error(s"Failed to retrieve ${ca.template.repository}. Aborting.")
      return 1
    }

    val name = ca.template.name getOrElse {
      try {
        Process("git config --global user.name").lines.toList(0)
      } catch {
        case e: java.io.IOException =>
          readLine("Please enter author's name: ")
      }
    }

    val organization = ca.template.packageName getOrElse {
      readLine(
        "Please enter the template's Scala package name (e.g. com.mycompany): ")
    }

    val email = ca.template.email getOrElse {
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
          sub(ca.template.repository, name, email, organization)
          valid = true
        case "n" | "N" =>
          meta(ca.template.repository, name, organization)
          valid = true
        case _ =>
          println("Please answer 'y' or 'n'")
          subscribe = readLine("(Y/n)? ")
      }
    } while (!valid)

    val repo = repos(ca.template.repository)

    println(s"Retrieving ${ca.template.repository}")
    val tags = read[List[GitHubTag]](repo.body)
    println(s"There are ${tags.size} tags")
    tags.headOption.map { tag =>
      println(s"Using the most recent tag ${tag.name}")
      val url =
        s"https://github.com/${ca.template.repository}/archive/${tag.name}.zip"
      println(s"Going to download ${url}")
      val trial = Http(url).asBytes
      val finalTrial = trial.location.map { loc =>
        println(s"Redirecting to ${loc}")
        Http(loc).asBytes
      } getOrElse trial
      val zipFilename =
        s"${ca.template.repository.replace('/', '-')}-${tag.name}.zip"
      FileUtils.writeByteArrayToFile(
        new File(zipFilename),
        finalTrial.body)
      val zis = new ZipInputStream(
        new BufferedInputStream(new FileInputStream(zipFilename)))
      val bufferSize = 4096
      val filesToModify = collection.mutable.ListBuffer[String]()
      var ze = zis.getNextEntry
      while (ze != null) {
        val filenameSegments = ze.getName.split(File.separatorChar)
        val destFilename = (ca.template.directory +: filenameSegments.tail).
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
            (nameOnly.endsWith(".scala") ||
              nameOnly == "build.sbt" ||
              nameOnly == "engine.json"))
            filesToModify += destFilename
        }
        ze = zis.getNextEntry
      }
      zis.close
      new File(zipFilename).delete

      val engineJsonFile =
        new File(ca.template.directory + File.separator + "engine.json")

      val engineJson = try {
        Some(parse(Source.fromFile(engineJsonFile).mkString))
      } catch {
        case e: java.io.IOException =>
          error("Unable to read engine.json. Skipping automatic package " +
            "name replacement.")
          None
        case e: MappingException =>
          error("Unable to parse engine.json. Skipping automatic package " +
            "name replacement.")
          None
      }

      val engineFactory = engineJson.map { ej =>
        (ej \ "engineFactory").extractOpt[String]
      } getOrElse None

      engineFactory.map { ef =>
        val pkgName = ef.split('.').dropRight(1).mkString(".")
        println(s"Replacing ${pkgName} with ${organization}...")

        filesToModify.foreach { ftm =>
          println(s"Processing ${ftm}...")
          val fileContent = Source.fromFile(ftm).getLines
          val processedLines =
            fileContent.map(_.replaceAllLiterally(pkgName, organization))
          FileUtils.writeStringToFile(
            new File(ftm),
            processedLines.mkString("\n"))
        }
      } getOrElse {
        error("engineFactory is not found in engine.json. Skipping automatic " +
          "package name replacement.")
      }

      println(s"Engine template ${ca.template.repository} is now ready at " +
        ca.template.directory)
    }

    0
  }
}
