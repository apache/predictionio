package io.prediction.data.sample

import io.prediction.data.storage.Storage
import io.prediction.data.view.LBatchView
import io.prediction.data.Utils

import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global

// engine's Data
class ItemTD(
  val iid: String,
  val itypes: Seq[String],
  val starttime: Option[Long],
  val endtime: Option[Long],
  val inactive: Boolean) extends Serializable {
    override def toString = s"${iid} ${itypes} ${starttime} ${endtime}" +
      s" ${inactive}"
  }

class UserTD(
  val uid: String
) extends Serializable {
  override def toString = s"${uid}"
}

class U2IActionTD(
  val uid: String,
  val iid: String,
  val action: String, // action name
  val v: Option[Int],
  val t: Long // action time
) extends Serializable {
  override def toString = s"${uid} ${iid} ${action} ${v} ${t}"
}

class TrainingData(
  val users: Map[String, UserTD], // uindex->uid
  val items: Map[String, ItemTD], // iindex->itemTD
  val u2iActions: Seq[U2IActionTD]
) extends Serializable {
  override def toString = s"u2iActions: ${u2iActions}\n" +
    s"users: ${users}\n" +
    s"items: ${items}\n"
}

// data source params
case class DataSourceParams(
  val appId: Int,
  val u2ievents: Set[String]
)

class DataSource(val params: DataSourceParams) {

  @transient lazy val eventsClient = Storage.eventClient("HB")

  private def stringToDateTime(dt: String): DateTime =
    Utils.stringToDateTime(dt)

  def readTraining(): TrainingData = {

    val result = eventsClient.getByAppId(params.appId)
    val eventsIter = result match {
      case Right(x) => x
      case Left(y) => Iterator()
    }

    val batchView = new LBatchView()
    val (userMap, itemMap) = batchView.entityPropertiesView(eventsIter)
      .partition{ case (k,v) => k.startsWith("u")}

    val users = userMap.map { case (k,v) => (k, new UserTD(uid=k)) }
    val items = itemMap.map { case (k,v) =>
      (k, new ItemTD(
        iid = k,
        itypes = v.getOrElse[List[String]]("pio_itypes", List()),
        //Seq(), // TODO: itypes from properties
        starttime = v.getOpt[String]("starttime")
          .map(j => stringToDateTime(j).getMillis),
        endtime = v.getOpt[String]("endtime")
          .map(j => stringToDateTime(j).getMillis),
        // TODO: how to support customizable field name ?
        inactive = v.getOrElse[Boolean]("inactive", false)
      ))}

    val result2 = eventsClient.getByAppId(params.appId)
    val u2iIter = result2 match {
      case Right(x) => x
      case Left(y) => Iterator()
    }

    val u2i = u2iIter.filter( e => params.u2ievents.contains(e.event) )
      .toList
      .map(e => new U2IActionTD(
        uid = e.entityId,
        iid = e.targetEntityId.get,
        action = e.event,
        // TODO: better way to handle type casting. Don't use JValue?
        v = e.properties.getOpt[Int]("pio_rate"),
        t = e.eventTime.getMillis
      ))

    new TrainingData(
      users = users,
      items = items,
      u2iActions = u2i
    )
  }

}

object DataSourceRun {

  def main(args: Array[String]) {

    val dsp = DataSourceParams(
      appId = args(0).toInt,
      u2ievents = Set("rate", "view"))

    val dataSource = new DataSource(dsp)
    val td = dataSource.readTraining()

    println(td)
  }

}
