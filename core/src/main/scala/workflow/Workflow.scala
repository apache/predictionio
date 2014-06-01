package io.prediction.workflow

import io.prediction.BaseAlgoParams
import io.prediction.BaseCleanserParams
//import io.prediction.BaseEvaluationParams
import io.prediction.BaseModel
import io.prediction.BaseServerParams
import io.prediction.BaseTrainingDataParams
import io.prediction.core.AbstractEngine
import io.prediction.core.AbstractEvaluator
//import io.prediction.core.BaseEvaluationSeq
//import io.prediction.core.BaseEvaluationUnitSeq
import io.prediction.core.BasePersistentData
import io.prediction.core.BasePredictionSeq
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{ Map => MMap, Set => MSet }
import scala.util.Random
import io.prediction.storage.Config
import io.prediction.storage.MongoSequences

import com.twitter.chill.MeatLocker
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy.Restart

object WorkflowScheduler {
  val tmpDir = "/tmp/pio/"

  def filename(id: Int): String = s"${tmpDir}${id}.data"

  def loadData(filePath: String): BasePersistentData = {
    val ois = new ObjectInputStream(new FileInputStream(filePath))
    val obj = ois.readObject
    obj.asInstanceOf[MeatLocker[BasePersistentData]].get
  }

  def saveData(id: Int, data: BasePersistentData): String = {
    val boxedData = MeatLocker(data)
    val filePath = filename(id)
    val oos = new ObjectOutputStream(new FileOutputStream(filePath))
    oos.writeObject(boxedData)
    filePath
  }
}

trait WorkflowScheduler {
  val runId: String

  def run(): Unit

  def loadData(filePath: String): BasePersistentData =
    WorkflowScheduler.loadData(filePath)

  def saveData(id: Int, data: BasePersistentData): String =
    WorkflowScheduler.saveData(id, data)
}

// This scheduler only run tasks that exists in database during start.
class SingleThreadScheduler(val runId: String) extends WorkflowScheduler {
  val config = new Config
  val tasksDb = new MongoTasks(config.workflowdataMongoDb.get)

  def runTask(task: Task, doneTasks: MMap[Int, Task]): Unit = {
    // gather data
    val localDataMap = task.dependingIds.map{
      id => (id, doneTasks(id).outputPath)
    }.toMap.mapValues(loadData)

    val taskOutput = task.run(localDataMap)

    val outputPath = saveData(task.id, taskOutput)

    // Book-keeping
    doneTasks += (task.id -> task)
    task.markDone(outputPath)
    // need to mark it in persistent database.
    tasksDb.markDone(task.id, outputPath)

    println(s"RunTask. id: ${task.id} task: $task")
  }

  def run(): Unit = {
    val doneTasks = MMap[Int, Task](tasksDb.getDone(runId).map(t => (t.id, t)):_*)
    val tasks = ArrayBuffer[Task](tasksDb.getNotDone(runId):_*)

    var changed = true
    while (changed) {
      changed = false

      // Find one task
      val taskOpt = tasks.find(t => {
        !t.done &&
        t.dependingIds.map(id => doneTasks.contains(id)).fold(true)(_ && _)
      })

      taskOpt.map{ task => {
        runTask(task, doneTasks)
        changed = true
      }}
    }
  }
}

class MultiThreadScheduler(val runId: String) extends WorkflowScheduler {
  def run(): Unit = {
    /**
     * Create an ActorSystem for a single workflow backed by a persistent task
     * storage.
     *
     * IMPORTANT: Persistent task storage is assumed to be fully atomic.
     *
     * Actors:
     *
     * - WorkflowSupervisor (parent of all)
     *
     *   This actor does not read from the persistent task storage and is
     *   responsible for restarting any BatchSupervisor that fails to access the
     *   persistent task storage.
     *
     *   Creates BatchSupervisors for all unfinished batches.
     *
     * - BatchSupervisor (child of WorkflowSupervisor)
     *
     *   Read in all tasks of a specific batch and create child actors to work
     *   on each individual task.
     *
     *   Create child TaskWorkers for each individual tasks.
     *
     *   Receiving Messages:
     *   - TaskDone()
     *     Receives this when a child TaskWorker has finished working. Updates
     *     the persistent task storage of the task's status. Figures out what
     *     task can be started and send a TaskStart() message to those
     *     TaskWorkers.
     *
     * - TaskWorker (child of BatchSupervisor)
     *
     *   Receiving Messages:
     *   - TaskStart()
     *     When received, start working on its task, and change its context to
     *     prevent double work.
     *
     *   Sending Messages:
     *   - TaskDone()
     *     When its own task is done, send this message to its BatchSupervisor.
     */
    val mtwfs = ActorSystem("pio-mt-wfs")
    val workflowSupervisor = mtwfs.actorOf(Props(classOf[WorkflowSupervisor], runId), "workflow-supervisor")
    mtwfs.awaitTermination()
  }
}

case class TaskStart(dependentTasksOutput: Map[Int, String])
case class TaskDone(id: Int, outputPath: String)
case object TaskAllDone

class WorkflowSupervisor(runId: String) extends Actor with ActorLogging {
  override val supervisorStrategy = OneForOneStrategy() {
    case t => Restart
  }

  // for now, only one batch is supported
  val batches = ArrayBuffer("single-batch")
  val batchSupervisors = batches map { b =>
    context.actorOf(Props(classOf[BatchSupervisor], runId), s"${b}-batch-supervisor")
  }

  def receive = {
    case TaskAllDone =>
      log.info("All tasks done. Yay!")
      context.system.shutdown()
  }
}

class BatchSupervisor(runId: String) extends Actor with ActorLogging {
  val config = new Config
  val tasksDb = new MongoTasks(config.workflowdataMongoDb.get)

  val doneTasks = MMap[Int, Task](tasksDb.getDone(runId).map(t => (t.id, t)): _*)
  val tasks = Map[Int, Task](tasksDb.getNotDone(runId).map(t => (t.id, t)): _*)

  override val supervisorStrategy = OneForOneStrategy() {
    case t => Restart
  }

  val taskWorkers: Map[Int, ActorRef] = tasks.map(t =>
    t._1 -> context.actorOf(Props(classOf[TaskWorker], t._2), s"task-worker-${t._1}")
  )

  startReadyTasks()

  def receive = {
    case TaskDone(id, outputPath) =>
      val task = tasks(id)
      doneTasks += (id -> task)
      task.markDone(outputPath)
      tasksDb.markDone(id, outputPath)
      startReadyTasks()
      if (tasks.keys.toSet == doneTasks.keys.toSet) context.parent ! TaskAllDone
  }

  def startReadyTasks(): Unit = {
    taskReadyToStart() foreach { tid =>
      val inputDataMap = tasks(tid).dependingIds.map(id =>
        (id, doneTasks(id).outputPath)
      ).toMap
      taskWorkers(tid) ! TaskStart(inputDataMap)
    }
  }

  def taskReadyToStart(): Seq[Int] = {
    val tasksToDo = tasks.filterKeys(!doneTasks.contains(_))
    tasksToDo.filter(t =>
      if (t._2.dependingIds.size == 0) true
      else t._2.dependingIds.map(doneTasks.contains).reduce(_ && _)
    ).keys.toSeq
  }
}

class TaskWorker(task: Task) extends Actor with ActorLogging {
  def receive = {
    case TaskStart(depTaskOutput) =>
      //if (scala.util.Random.nextInt(5) == 0) throw new RuntimeException("random oops!")
      val localDataMap = depTaskOutput.mapValues(WorkflowScheduler.loadData)
      val taskOutput = task.run(localDataMap)
      val outputPath = WorkflowScheduler.saveData(task.id, taskOutput)
      log.info(s"TaskWorker (id: ${task.id}, task: $task)")
      sender ! TaskDone(task.id, outputPath)
      context.become(finished, true)
  }

  def finished: Receive = {
    case TaskStart(depTaskOutput) =>
  }
}

class WorkflowSubmitter {
  val config = new Config
  val tasksDb = new MongoTasks(config.workflowdataMongoDb.get)

  def nextId(): Int = tasksDb.nextId

  def submit(task: Task): Int = {
    println(s"SubmitTask id: ${task.id} depends: ${task.dependingIds} task: $task")
    tasksDb.insert(task)
    task.id
  }
}

object Run {
  def main(args: Array[String]): Unit = {
    val s = new SingleThreadScheduler("")
    s.run
  }
}
