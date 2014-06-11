package io.prediction.workflow

// Havn't decide where to put this module. Maybe move to i.p.storage.

import com.mongodb.casbah.Imports.MongoCursor
import com.mongodb.casbah.commons.Imports._
import com.mongodb.casbah.Imports.MongoDB
import com.twitter.chill.MeatLocker
import io.prediction.storage.MongoSequences

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

trait Tasks {
  def nextId(): Int

  def insert(task: Task): Unit

  def get(taskId: Int): Option[Task]

  def markDone(taskId: Int, outputPath: String): Unit

  def getNotDone(runId: String): Seq[Task]

  def getDone(runId: String): Seq[Task]

  def getDoneIds(runId: String): Seq[Int]
}

class MongoTasks(db: MongoDB) extends Tasks {
  private val taskColl = db("tasks")

  val mongoSeq = new MongoSequences(db)
  def nextId(): Int = mongoSeq.genNext("workflow")

  private def upsert(taskId: Int, task: Task, upsert: Boolean = false): Unit = {
    val queryObj = MongoDBObject("_id" -> taskId)
    val updateObj = MongoDBObject(
      "_id" -> task.id,
      "batch" -> task.batch,
      "runId" -> task.runId,
      "dependingIds" -> task.dependingIds,
      "done" -> task.done,
      "data" -> taskToByteArray(task),
      "debugName" -> task.toString
    )
    taskColl.update(queryObj, updateObj, upsert)
  }

  def insert(task: Task): Unit = upsert(task.id, task, /* upsert */ true)

  def markDone(taskId: Int, outputPath: String): Unit = {
    val task = get(taskId).get
    task.markDone(outputPath)
    upsert(task.id, task, upsert = false)
  }

  def get(taskId: Int): Option[Task] = {
    val taskOpt = taskColl.findOne(MongoDBObject("_id" -> taskId))
    taskOpt.map(taskObj => byteArrayToTask(taskObj.as[Array[Byte]]("data")))
  }

  def getNotDone(runId: String): Seq[Task] = {
    val queryObj = MongoDBObject("runId" -> runId, "done" -> false)
    (new MongoTasksIterator(taskColl.find(queryObj))).toSeq
  }

  def getDone(runId: String): Seq[Task] = {
    val queryObj = MongoDBObject("runId" -> runId, "done" -> true)
    (new MongoTasksIterator(taskColl.find(queryObj))).toSeq
  }

  def getDoneIds(runId: String): Seq[Int] = {
    val queryObj = MongoDBObject("runId" -> runId, "done" -> true)
    val fieldsObj = MongoDBObject("_id" -> 1)
    taskColl.find(queryObj, fieldsObj).map(_.as[Int]("_id")).toSeq
  }

  private def taskToByteArray(task: Task): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(MeatLocker(task.asInstanceOf[Task]))
    bos.toByteArray
  }

  private def byteArrayToTask(bs: Array[Byte]): Task = {
    val bos = new ByteArrayInputStream(bs)
    val oos = new ObjectInputStream(bos)
    val task = oos.readObject.asInstanceOf[MeatLocker[Task]].get
    task
  }

  class MongoTasksIterator(it: MongoCursor) extends Iterator[Task] {
    def next = byteArrayToTask(it.next.as[Array[Byte]]("data"))
    def hasNext = it.hasNext
  }
}
