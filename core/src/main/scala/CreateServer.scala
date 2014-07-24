package io.prediction.deploy

import io.prediction.controller.IEngineFactory
import io.prediction.controller.EmptyParams
import io.prediction.controller.Engine
import io.prediction.controller.PAlgorithm
import io.prediction.controller.Params
import io.prediction.controller.java.LJavaAlgorithm
//import io.prediction.PersistentParallelModel
import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseServing
import io.prediction.core.Doer
import io.prediction.storage.{ Storage, EngineManifest, Run }
import io.prediction.workflow.EI
import io.prediction.workflow.EngineLanguage
import io.prediction.workflow.WorkflowContext
import io.prediction.workflow.WorkflowUtils

import akka.actor.{ Actor, ActorSystem, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.google.gson.Gson
import com.twitter.chill.KryoInjection
import com.twitter.chill.ScalaKryoInstantiator
import grizzled.slf4j.Logging
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization.{ read, write }
import spray.can.Http
import spray.routing._
import spray.http._
import spray.http.MediaTypes._

import scala.concurrent.duration._
import scala.language.existentials
import scala.reflect.runtime.universe

import java.io.File
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.net.URLClassLoader

class KryoInstantiator(classLoader: ClassLoader) extends ScalaKryoInstantiator {
  override def newKryo = {
    val kryo = super.newKryo
    kryo.setClassLoader(classLoader)
    kryo
  }
}

case class ServerConfig(
  runId: String = "",
  engineId: Option[String] = None,
  engineVersion: Option[String] = None,
  ip: String = "localhost",
  port: Int = 8000,
  sparkMaster: String = "local",
  sparkExecutorMemory: String = "4g")

object CreateServer extends Logging {
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[ServerConfig]("RunServer") {
      opt[String]("engineId") action { (x, c) =>
        c.copy(engineId = Some(x))
      } text("Engine ID.")
      opt[String]("version") action { (x, c) =>
        c.copy(engineVersion = Some(x))
      } text("Engine version.")
      opt[String]("ip") action { (x, c) =>
        c.copy(ip = x)
      } text("IP to bind to (default: localhost).")
      opt[Int]("port") action { (x, c) =>
        c.copy(port = x)
      } text("Port to bind to (default: 8000).")
      opt[String]("sparkMaster") action { (x, c) =>
        c.copy(sparkMaster = x)
      } text("Apache Spark master URL (default: local).")
      opt[String]("sparkExecutorMemory") action { (x, c) =>
        c.copy(sparkExecutorMemory = x)
      } text("Apache Spark executor memory (default: 4g)")
      opt[String]("runId") required() action { (x, c) =>
        c.copy(runId = x)
      } text("Run ID.")
    }

    parser.parse(args, ServerConfig()) map { sc =>
      val runs = Storage.getMetaDataRuns
      val engineManifests = Storage.getMetaDataEngineManifests
      runs.get(sc.runId) map { run =>
        val engineId = sc.engineId.getOrElse(run.engineId)
        val engineVersion = sc.engineVersion.getOrElse(run.engineVersion)
        engineManifests.get(engineId, engineVersion) map { manifest =>

          val engineFactoryName = manifest.engineFactory
          val (engineLanguage, engine) =
            WorkflowUtils.getEngine(engineFactoryName, getClass.getClassLoader)

          /*
          val ppmExists = models.head.exists(
            _.isInstanceOf[PersistentParallelModel])

          info(s"Persistent parallel model exists? ${ppmExists}")

          val sc: Option[SparkContext] = if (ppmExists) {
            val conf = new SparkConf()
            conf.setAppName(
              s"PredictionIO Server: ${manifest.id} ${manifest.version}")
            if (parsed.sparkMaster != "")
              conf.setMaster(parsed.sparkMaster)
            conf.set("spark.executor.memory", parsed.sparkExecutorMemory)
            Some(new SparkContext(conf))
          } else None

          models.head foreach {
            case ppm: PersistentParallelModel =>
              info(s"Loading persisted parallel model: ${ppm.getClass.getName}")
              ppm.load(sc.get, run.id)
            case _ =>
          }
          */

          createServerWithEngine(
            sc = sc,
            run = run,
            engine = engine,
            engineLanguage = engineLanguage,
            manifest = manifest)

        } getOrElse {
          error(s"Invalid Engine ID or version. Aborting server.")
        }
      } getOrElse {
        error(s"Invalid Run ID. Aborting server.")
      }
    }
  }

  def createServerWithEngine[TD, DP, PD, Q, P, A](
    sc: ServerConfig,
    run: Run,
    engine: Engine[TD, DP, PD, Q, P, A],
    engineLanguage: EngineLanguage.Value,
    manifest: EngineManifest): Unit = {
    implicit val formats = DefaultFormats
    val algorithmsParamsWithNames =
      read[Seq[(String, JValue)]](run.algorithmsParams).map {
        case (algoName, params) =>
          val extractedParams = WorkflowUtils.extractParams(
            engineLanguage,
            compact(render(params)),
            engine.algorithmClassMap(algoName))
          (algoName, extractedParams)
      }

    val algorithmsParams = algorithmsParamsWithNames.map { _._2 }

    val algorithms = algorithmsParamsWithNames.map { case (n, p) =>
      Doer(engine.algorithmClassMap(n), p)
    }

    val servingParams =
      if (run.servingParams == "")
        EmptyParams()
      else
        WorkflowUtils.extractParams(
          engineLanguage,
          run.servingParams,
          engine.servingClass)
    val serving = Doer(engine.servingClass, servingParams)

    val pAlgorithmExists = algorithms.exists(_.isInstanceOf[PAlgorithm[_, PD, _, Q, P]])
    val sparkContext = if (pAlgorithmExists) Some(WorkflowContext(run.batch, run.env)) else None
    val evalPreparedMap = sparkContext.map { sc =>
      logger.info("Data Source")
      val dataSourceParams = WorkflowUtils.extractParams(
        engineLanguage,
        run.dataSourceParams,
        engine.dataSourceClass)
      val dataSource = Doer(engine.dataSourceClass, dataSourceParams)
      val evalParamsDataMap
      : Map[EI, (DP, TD, RDD[(Q, A)])] = dataSource
        .readBase(sc)
        .zipWithIndex
        .map(_.swap)
        .toMap
      val evalDataMap: Map[EI, (TD, RDD[(Q, A)])] = evalParamsDataMap.map {
        case(ei, e) => (ei -> (e._2, e._3))
      }
      logger.info("Preparator")
      val preparatorParams = WorkflowUtils.extractParams(
        engineLanguage,
        run.preparatorParams,
        engine.preparatorClass)
      val preparator = Doer(engine.preparatorClass, preparatorParams)

      val evalPreparedMap: Map[EI, PD] = evalDataMap
      .map{ case (ei, data) => (ei, preparator.prepareBase(sc, data._1)) }
      logger.info("Preparator complete")

      evalPreparedMap
    }

    val kryoInstantiator = new KryoInstantiator(getClass.getClassLoader)
    val kryo = KryoInjection.instance(kryoInstantiator)
    val modelsFromRun = kryo.invert(run.models).get.asInstanceOf[Seq[Seq[Any]]]
    val models = modelsFromRun.head.zip(algorithms).map {
      case (m, a) =>
        if (a.isInstanceOf[PAlgorithm[_, _, _, Q, P]]) {
          info(s"Parallel model detected for algorithm ${a.getClass.getName}")
          a.trainBase(sparkContext.get, evalPreparedMap.get(0))
        } else {
          try {
            info(s"Loaded model ${m.getClass.getName} for algorithm ${a.getClass.getName}")
            m
          } catch {
            case e: NullPointerException =>
              warn(s"Null model detected for algorithm ${a.getClass.getName}")
              m
          }
        }
    }

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("predictionio-server")

    // create and start our service actor
    val service = system.actorOf(
      Props(
        classOf[ServerActor[Q, P]],
        sc,
        run,
        engine,
        engineLanguage,
        manifest,
        algorithms,
        algorithmsParams,
        models,
        serving,
        servingParams),
      "server")

    implicit val timeout = Timeout(5.seconds)
    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(service, interface = sc.ip, port = sc.port)
  }
}

class ServerActor[Q, P](
    val args: ServerConfig,
    val run: Run,
    val engine: Engine[_, _, _, Q, P, _],
    val engineLanguage: EngineLanguage.Value,
    val manifest: EngineManifest,
    val algorithms: Seq[BaseAlgorithm[_ <: Params, _, _, Q, P]],
    val algorithmsParams: Seq[Params],
    val models: Seq[Any],
    val serving: BaseServing[_ <: Params, Q, P],
    val servingParams: Params) extends Actor with HttpService {

  lazy val gson = new Gson

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <head>
                <title>{manifest.id} {manifest.version} - PredictionIO Server at {args.ip}:{args.port}</title>
              </head>
              <body>
                <h1>PredictionIO Server at {args.ip}:{args.port}</h1>
                <div>
                  <ul>
                    <li><strong>Run ID:</strong> {args.runId}</li>
                    <li><strong>Run Start Time:</strong> {run.startTime}</li>
                    <li><strong>Run End Time:</strong> {run.endTime}</li>
                    <li><strong>Engine:</strong> {manifest.id} {manifest.version}</li>
                    <li><strong>Class:</strong> {manifest.engineFactory}</li>
                    <li><strong>Files:</strong> {manifest.files.mkString(" ")}</li>
                    <li><strong>Algorithms:</strong> {algorithms.mkString(" ")}</li>
                    <li><strong>Algorithms Parameters:</strong> {algorithmsParams.mkString(" ")}</li>
                    <li><strong>Models:</strong> {models.mkString(" ")}</li>
                    <li><strong>Server Parameters:</strong> {servingParams}</li>
                  </ul>
                </div>
              </body>
            </html>
          }
        }
      } ~
      post {
        entity(as[String]) { queryString =>
          val firstAlgorithm = algorithms.head
          val query =
            if (firstAlgorithm.isInstanceOf[LJavaAlgorithm[_, _, _, Q, P]]) {
              gson.fromJson(
                queryString,
                firstAlgorithm.asInstanceOf[LJavaAlgorithm[_, _, _, Q, P]].
                  queryClass)
            } else {
              Extraction.extract(parse(queryString))(
                firstAlgorithm.querySerializer,
                firstAlgorithm.queryManifest)
            }
          val predictions = algorithms.zipWithIndex.map { case (a, ai) =>
            a.predictBase(models(ai), query)
          }
          val prediction = serving.serveBase(query, predictions)
          complete(compact(render(
            Extraction.decompose(prediction)(firstAlgorithm.querySerializer))))
        }
      }
    }
}
