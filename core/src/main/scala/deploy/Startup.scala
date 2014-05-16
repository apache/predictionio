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

trait Startup {

  def systemName = "pio"
  def workTimeout = 10.seconds

  def startBackend(hostname: String, port: Int,
    joinAddressOption: Option[Address],
    role: String): (ActorSystem, Address) = {
    val conf = ConfigFactory.parseString(s"""
      akka.cluster.roles=[$role]
      akka.remote.netty.tcp.hostname=$hostname
      akka.remote.netty.tcp.port=$port
      """).
      withFallback(ConfigFactory.load())
    val system = ActorSystem(systemName, conf)
    val joinAddress = joinAddressOption.getOrElse(Cluster(system).selfAddress)
    Cluster(system).join(joinAddress)
    system.actorOf(ClusterSingletonManager.props(Master.props(workTimeout),
      "active", PoisonPill, Some(role)), "master")
    (system, joinAddress)
  }

  def startWorker(hostname: String, port: Int): ActorSystem = {
    val contactAddress = Address("akka.tcp", systemName, hostname, port)
    val system = ActorSystem(systemName)
    val initialContacts = Set(system.actorSelection(
      RootActorPath(contactAddress) / "user" / "receptionist"))
    val clusterClient = system.actorOf(
      ClusterClient.props(initialContacts), "clusterClient")
    system.actorOf(Worker.props(clusterClient, Props[WorkExecutor]), "worker")
    system
  }

  def startFrontend(hostname: String, port: Int): ActorSystem = {
    val joinAddress = Address("akka.tcp", systemName, hostname, port)
    val system = ActorSystem(systemName)
    Cluster(system).join(joinAddress)
    val frontend = system.actorOf(Props[Frontend], "frontend")
    system.actorOf(Props(classOf[WorkProducer], frontend), "producer")
    system.actorOf(Props[WorkResultConsumer], "consumer")
    system
  }
}
