package io.prediction.scheduler

import org.quartz.{ JobExecutionContext, JobExecutionException, JobKey, SchedulerException }
import org.quartz.listeners.JobListenerSupport

class JobTreeJobListener(name: String) extends JobListenerSupport {
  private var treeLinks = collection.mutable.Map[JobKey, collection.mutable.ListBuffer[JobKey]]()

  def getName(): String = {
    name
  }

  def addJobTreeLink(firstJob: JobKey, secondJob: JobKey) {
    treeLinks.get(firstJob) map { links =>
      links += secondJob
    } getOrElse {
      treeLinks += (firstJob -> collection.mutable.ListBuffer(secondJob))
    }
  }

  override def jobWasExecuted(context: JobExecutionContext, jobException: JobExecutionException) = {
    val executedJobKey = context.getJobDetail().getKey()

    treeLinks.get(executedJobKey) map { links =>
      links foreach { link =>
        getLog().info("Job '" + executedJobKey + "' will now link to Job '" + link + "'")

        try {
          context.getScheduler().triggerJob(link)
        } catch {
          case se: SchedulerException => getLog().error("Error encountered during chaining to Job '" + link + "'", se)
        }
      }
    }
  }
}
