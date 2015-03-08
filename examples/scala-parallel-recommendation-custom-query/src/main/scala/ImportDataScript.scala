package org.template.recommendation

import java.io.File
import java.util

import io.prediction.EngineClient
import io.prediction.EventClient
import io.prediction.Event

import scala.collection.parallel.ParIterable
import scala.io.Source
import scala.collection.JavaConverters._

object ImportDataScript extends App {
  private val CreationYear = "creationYear"
  private val MovieTitle = "title"
  private val Rate = "rate"
  private val User = "user"
  private val Movie = "movie"

  override def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      throw new IllegalArgumentException(
        "access key should be passed to import client")
    }

    val accessKey = args(0)
    val engineUrl = if (args.length > 1) args(1) else "localhost:8000"
    implicit val client = new EventClient(accessKey, engineUrl)
    println(s"imported ${importMovies} movies")
    println(s"imported ${importUsers} users")
    println(s"imported ${importRateEvents} events")
    System.exit(0)
  }

  /**
   * imports ivents to the pio server.
   * @return the events id list.
   */
  def importRateEvents(implicit client: EventClient): Int =
    readCSV("data/u.data").flatMap { event => withDefaultErrorHandling {
      val Array(userId, itemId, rating, timestamp) = event
      client.createEvent((new Event)
          .event(Rate)
          .entityId(userId)
          .entityType(User)
          .targetEntityId(itemId)
          .targetEntityType(Movie))
    }}.sum


  def importUsers(implicit client: EventClient): Int =
    readCSV("data/u.user").flatMap { user => withDefaultErrorHandling {
      val Array(userId, _ *) = user
      client.setUser(userId, Map.empty[String, AnyRef].asJava)
    }}.sum

  /**
   * imports movies to pio server
   * @return the number if movies where imported
   */
  def importMovies(implicit client: EventClient): Int =
    readCSV("data/u.item").flatMap { movie => withDefaultErrorHandling {
      val Array(movieId, movieTitle, releaseDate, _ *) = movie
      val releaseYear = releaseDate.split("-")(2).toInt
      client.setItem(movieId, Map(CreationYear -> new Integer(releaseYear),
        MovieTitle -> movieTitle).toMap[String, AnyRef].asJava)
    }}.sum

  /**
   * reads csv file into list of string arrays each of them represents splitted
   * with specified delimeter line
   * @param filename path to csv file
   * @param delimiter delimiter of the properties in the file
   * @return the list of string arrays made from every file line
   */
  private def readCSV(filename: String, delimiter: String = "\t") =
    Source.fromFile(filename).getLines().map(_.split(delimiter))

  private def withDefaultErrorHandling(f: => Unit) =
    try { f; Some(1) }
    catch { case e: Throwable => println(e.toString); None }
}