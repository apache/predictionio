package io.prediction.data.storage.hbase.upgrade

import grizzled.slf4j.Logger
import io.prediction.data.storage.Storage
import io.prediction.data.storage.hbase.HBLEvents
import io.prediction.data.storage.hbase.HBEventsUtil

import scala.collection.JavaConversions._

import scala.concurrent._
import ExecutionContext.Implicits.global
import io.prediction.data.storage.LEvents
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.lang.Thread

object CheckDistribution {
  def entityType(eventClient: LEvents, appId: Int)
  : Map[(String, Option[String]), Int] = {
    eventClient
    .find(appId = appId)
    .right
    .get
    .foldLeft(Map[(String, Option[String]), Int]().withDefaultValue(0)) {
      case (m, e) => {
        val k = (e.entityType, e.targetEntityType)
        m.updated(k, m(k) + 1)
      }
    }
  }

  def runMain(appId: Int) {
    val eventClient = Storage.getLEvents().asInstanceOf[HBLEvents]

    entityType(eventClient, appId)
    .toSeq
    .sortBy(-_._2)
    .foreach { println }

  }

  def main(args: Array[String]) {
    runMain(args(0).toInt)
  }

}

/* Experimental */
object Upgrade_0_8_3 {
  val NameMap = Map(
    "pio_user" -> "user",
    "pio_item" -> "item")
  val RevNameMap = NameMap.toSeq.map(_.swap).toMap

  val logger = Logger[this.type]

  def main(args: Array[String]) {
    val fromAppId = args(0).toInt
    val toAppId = args(1).toInt

    runMain(fromAppId, toAppId)
  }

  def runMain(fromAppId: Int, toAppId: Int) = {
    upgrade(fromAppId, toAppId)
  }

  def isEmpty(eventClient: LEvents, appId: Int): Boolean =
    !eventClient.find(appId = appId).right.get.hasNext


  def upgradeCopy(eventClient: LEvents, fromAppId: Int, toAppId: Int) {
    val fromDist = CheckDistribution.entityType(eventClient, fromAppId)

    logger.info("FromAppId Distribution")
    fromDist.toSeq.sortBy(-_._2).foreach { e => logger.info(e) }

    val events = eventClient
    .find(appId = fromAppId)
    .right
    .get
    .zipWithIndex
    .foreach { case (fromEvent, index) => {
      if (index % 50000 == 0) {
        //logger.info(s"Progress: $fromEvent $index")
        logger.info(s"Progress: $index")
      }


      val fromEntityType = fromEvent.entityType
      val toEntityType = NameMap.getOrElse(fromEntityType, fromEntityType)

      val fromTargetEntityType = fromEvent.targetEntityType
      val toTargetEntityType = fromTargetEntityType
        .map { et => NameMap.getOrElse(et, et) }

      val toEvent = fromEvent.copy(
        entityType = toEntityType,
        targetEntityType = toTargetEntityType)

      eventClient.insert(toEvent, toAppId)
    }}


    val toDist = CheckDistribution.entityType(eventClient, toAppId)

    logger.info("Recap fromAppId Distribution")
    fromDist.toSeq.sortBy(-_._2).foreach { e => logger.info(e) }

    logger.info("ToAppId Distribution")
    toDist.toSeq.sortBy(-_._2).foreach { e => logger.info(e) }

    val fromGood = fromDist
      .toSeq
      .forall { case (k, c) => {
        val (et, tet) = k
        val net = NameMap.getOrElse(et, et)
        val ntet = tet.map(tet => NameMap.getOrElse(tet, tet))
        val nk = (net, ntet)
        val nc = toDist.getOrElse(nk, -1)
        val checkMatch = (c == nc)
        if (!checkMatch) {
          logger.info(s"${k} doesn't match: old has ${c}. new has ${nc}.")
        }
        checkMatch
      }}

    val toGood = toDist
      .toSeq
      .forall { case (k, c) => {
        val (et, tet) = k
        val oet = RevNameMap.getOrElse(et, et)
        val otet = tet.map(tet => RevNameMap.getOrElse(tet, tet))
        val ok = (oet, otet)
        val oc = fromDist.getOrElse(ok, -1)
        val checkMatch = (c == oc)
        if (!checkMatch) {
          logger.info(s"${k} doesn't match: new has ${c}. old has ${oc}.")
        }
        checkMatch
      }}

    if (!fromGood || !toGood) {
      logger.error("Doesn't match!! There is an import error.")
    } else {
      logger.info("Count matches. Looks like we are good to go.")
    }
  }

  /* For upgrade from 0.8.2 to 0.8.3 only */
  def upgrade(fromAppId: Int, toAppId: Int) {

    val eventClient = Storage.getLEvents().asInstanceOf[HBLEvents]

    require(fromAppId != toAppId,
      s"FromAppId: $fromAppId must be different from toAppId: $toAppId")

    require(
      isEmpty(eventClient, toAppId),
      s"Target appId: $toAppId is not empty. Please run " +
      "`pio app data-delete <app_name>` to clean the data before upgrading")


    logger.info(s"$fromAppId isEmpty: " + isEmpty(eventClient, fromAppId))

    upgradeCopy(eventClient, fromAppId, toAppId)

  }


}
