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

trait WorkflowScheduler {
  val tmpDir = "/tmp/pio/"

  def run(): Unit

  def filename(id: Int): String = s"${tmpDir}${id}.data"
}

// This scheduler only run tasks that exists in database during start.
class SingleThreadScheduler extends WorkflowScheduler {
  val config = new Config
  val tasksDb = new MongoTasks(config.workflowdataMongoDb.get)

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
    val doneTasks = MMap[Int, Task](tasksDb.getDone.map(t => (t.id, t)):_*)
    val tasks = ArrayBuffer[Task](tasksDb.getNotDone():_*)

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

class WorkflowSubmitter {
  val config = new Config
  val tasksDb = new MongoTasks(config.workflowdataMongoDb.get)

  def nextId(): Int = tasksDb.nextId

  def submit(task: Task): Int = {
    println(s"Task id: ${task.id} depends: ${task.dependingIds} task: $task")
    tasksDb.insert(task)
    task.id
  }
}

object Run {
  def main(args: Array[String]): Unit = {
    val s = new SingleThreadScheduler
    s.run
  }
}
