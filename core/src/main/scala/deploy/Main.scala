package io.prediction.core.deploy

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Address
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.RootActorPath
import akka.cluster.Cluster
import akka.contrib.pattern.ClusterClient
import akka.contrib.pattern.ClusterSingletonManager

import master._
import worker._

object Main extends Startup {

  def main(args: Array[String]): Unit = {
    val joinAddress = startBackend(None, "backend")
    Thread.sleep(5000)
    startBackend(Some(joinAddress), "backend")
    startWorker(joinAddress)
    Thread.sleep(5000)
    startFrontend(joinAddress)
  }

}

trait Startup {

  def systemName = "Workers"
  def workTimeout = 10.seconds

  def startBackend(joinAddressOption: Option[Address], role: String): Address = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.load())
    val system = ActorSystem(systemName, conf)
    val joinAddress = joinAddressOption.getOrElse(Cluster(system).selfAddress)
    Cluster(system).join(joinAddress)
    system.actorOf(ClusterSingletonManager.props(Master.props(workTimeout), "active",
      PoisonPill, Some(role)), "master")
    joinAddress
  }

  def startWorker(contactAddress: akka.actor.Address): Unit = {
    val system = ActorSystem(systemName)
    val initialContacts = Set(
      system.actorSelection(RootActorPath(contactAddress) / "user" / "receptionist"))
    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    system.actorOf(Worker.props(clusterClient, Props[WorkExecutor]), "worker")
  }

  def startFrontend(joinAddress: akka.actor.Address): Unit = {
    val system = ActorSystem(systemName)
    Cluster(system).join(joinAddress)
    val frontend = system.actorOf(Props[Frontend], "frontend")
    system.actorOf(Props(classOf[WorkProducer], frontend), "producer")
    system.actorOf(Props[WorkResultConsumer], "consumer")
  }
}
