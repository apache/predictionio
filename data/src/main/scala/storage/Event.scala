package io.prediction.data.storage

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global // TODO

case class Event(
  val entityId: String,
  val targetEntityId: Option[String] = None,
  val event: String,
  val properties: DataMap = DataMap(), //Map[String, JValue] = Map(),
  val eventTime: DateTime = DateTime.now, // default to current time
  val tags: Seq[String] = Seq(),
  val appId: Int,
  val predictionKey: Option[String] = None
) {
  require(!entityId.isEmpty, "entityId must not be empty string.")
  require(targetEntityId.map(!_.isEmpty).getOrElse(true),
    "targetEntityId must not be empty string.")
  require(!event.isEmpty, "event must not be empty.")
}


trait Events {

  private def notImplemented = Future {
    Left(StorageError("Not implemented."))
  }
  val timeout = Duration(5, "seconds")

  def futureInsert(event: Event): Future[Either[StorageError, String]] =
    notImplemented

  def futureGet(eventId: String): Future[Either[StorageError, Option[Event]]] =
    notImplemented

  def futureDelete(eventId: String): Future[Either[StorageError, Boolean]] =
    notImplemented

  def futureGetByAppId(appId: Int):
    Future[Either[StorageError, Iterator[Event]]] = notImplemented

  /* where t >= start and t < untilTime */
  def futureGetByAppIdAndTime(appId: Int,
    startTime: Option[DateTime], untilTime: Option[DateTime]):
    Future[Either[StorageError, Iterator[Event]]] = notImplemented

  def futureDeleteByAppId(appId: Int): Future[Either[StorageError, Unit]] =
    notImplemented

  // following is blocking
  def insert(event: Event): Either[StorageError, String] = {
    Await.result(futureInsert(event), timeout)
  }

  def get(eventId: String): Either[StorageError, Option[Event]] = {
    Await.result(futureGet(eventId), timeout)
  }

  def delete(eventId: String): Either[StorageError, Boolean] = {
    Await.result(futureDelete(eventId), timeout)
  }

  def getByAppId(appId: Int): Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppId(appId), timeout)
  }

  def getByAppIdAndTime(appId: Int, startTime: Option[DateTime],
    untilTime: Option[DateTime]): Either[StorageError, Iterator[Event]] = {
    Await.result(futureGetByAppIdAndTime(appId, startTime, untilTime), timeout)
  }

  def deleteByAppId(appId: Int): Either[StorageError, Unit] = {
    Await.result(futureDeleteByAppId(appId), timeout)
  }

}
