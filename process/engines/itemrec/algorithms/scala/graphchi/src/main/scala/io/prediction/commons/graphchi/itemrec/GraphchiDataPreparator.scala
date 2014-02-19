package io.prediction.commons.graphchi.itemrec

import io.prediction.commons.Config
import io.prediction.commons.appdata.{Item, U2IAction, User}

import grizzled.slf4j.Logger
import java.io.File
import java.io.FileWriter
import java.io.RandomAccessFile
import java.io.BufferedWriter
import scala.io.Source

import com.twitter.scalding.Args

object GraphChiDataPreparator {

  /* constants */
  final val ACTION_RATE = "rate"
  final val ACTION_LIKE = "like"
  final val ACTION_DISLIKE = "dislike"
  final val ACTION_VIEW = "view"
  final val ACTION_CONVERSION = "conversion"

  // When there are conflicting actions, e.g. a user gives an item a rating 5 but later dislikes it, 
  // determine which action will be considered as final preference.
  final val CONFLICT_LATEST: String = "latest" // use latest action
  final val CONFLICT_HIGHEST: String = "highest" // use the one with highest score
  final val CONFLICT_LOWEST: String = "lowest" // use the one with lowest score
  
  /* global */
  val logger = Logger(GraphChiDataPreparator.getClass)

  println(logger.isInfoEnabled)

  val commonsConfig = new Config
  val usersDb = commonsConfig.getAppdataUsers
  val itemsDb = commonsConfig.getAppdataItems
  val u2iDb = commonsConfig.getAppdataU2IActions

  // argument of this job
  case class JobArg (
    val outputDir: String,
    val appid: Int,
    val evalid: Option[Int],
    val itypes: Option[List[String]],
    val viewParam: Option[Int],
    val likeParam: Option[Int],
    val dislikeParam: Option[Int],
    val conversionParam: Option[Int],
    val conflictParam: String,
    val matrixMarket: Boolean
  )

  def main(cmdArgs: Array[String]) {

    println("Running data preparator for GraphChi...")
    println(cmdArgs.mkString(","))

    /* get arg */
    val args = Args(cmdArgs)

    val outputDirArg = args("outputDir")
    val appidArg = args("appid").toInt
    val evalidArg = args.optional("evalid") map (x => x.toInt)
    val OFFLINE_EVAL = (evalidArg != None) // offline eval mode

    val preItypesArg = args.list("itypes")
    val itypesArg: Option[List[String]] = if (preItypesArg.mkString(",").length == 0) None else Option(preItypesArg)

    // determine how to map actions to rating values
    def getActionParam(name: String): Option[Int] = {
      val actionParam: Option[Int] = args(name) match {
        case "ignore" => None
        case x => Some(x.toInt)
      }
      actionParam
    }

    val viewParamArg: Option[Int] = getActionParam("viewParam")
    val likeParamArg: Option[Int] = getActionParam("likeParam")
    val dislikeParamArg: Option[Int] = getActionParam("dislikeParam")
    val conversionParamArg: Option[Int] = getActionParam("conversionParam")

    val conflictParamArg: String = args("conflictParam")

    // check if the conflictParam is valid
    require(List(CONFLICT_LATEST, CONFLICT_HIGHEST, CONFLICT_LOWEST).contains(conflictParamArg), "conflict param " + conflictParamArg + " is not valid.")

    val matrixMarketArg: Boolean = args.optional("matrixMarket").map(x => x.toBoolean).getOrElse(true)

    val arg = JobArg(
      outputDir = outputDirArg,
      appid = appidArg,
      evalid = evalidArg,
      itypes = itypesArg,
      viewParam = viewParamArg,
      likeParam = likeParamArg,
      dislikeParam = dislikeParamArg,
      conversionParam = conversionParamArg,
      conflictParam = conflictParamArg,
      matrixMarket = matrixMarketArg
    )

    /* run job */
    dataPrep(arg)
    cleanup(arg)

  }

  def dataPrep(arg: JobArg) = {
    // TODO: if offline eval, use evalid as appid

    // TODO: create outputDir if doesn't exist yet.

    /* write user index */
    val usersIndexWriter = new BufferedWriter(new FileWriter(new File(arg.outputDir+"usersIndex.tsv")))
    // TODO sort by ID when read from Mongo (although not needed for funtionality)
    // convert to Map for later lookup
    // assuming number of users can be fit into memory.
    val usersIndex: Map[String, Int] = usersDb.getByAppid(arg.appid).map( _.id ).zipWithIndex
      .map{ case (uid, index) => (uid, index+1) }.toMap // +1 to make it starting from 1
 
    // TODO: output with key order?
    usersIndex.foreach { case (uid, uindex) =>
      usersIndexWriter.write(s"${uindex}\t${uid}\n")
    }
    usersIndexWriter.close()

    /* write items and ratings */
    val itemsIndexWriter = new BufferedWriter(new FileWriter(new File(arg.outputDir+"itemsIndex.tsv")))
    val ratingsWriter = new BufferedWriter(new FileWriter(new File(arg.outputDir+"ratings.csv"))) // intermediate file
    val ratingsTempWriter = new BufferedWriter(new FileWriter(new File(arg.outputDir+"ratingsTemp.tsv"))) // TODO: remove

    // TODO: add itypes, sort by ID when read from Mongo
    val itemsIndex: Iterator[(String, Int)] = itemsDb.getByAppid(arg.appid).map( _.id ).zipWithIndex
      .map{ case (iid, index) => (iid, index+1) } // +1 to make it starting from 1
    
    var numberOfEntries = 0
    var numberOfItems = 0
    itemsIndex.foreach { case (iid, iindex) =>
      // TODO: also write extra item data (itypes, starttime, endtime, etc) into itemsindex
      itemsIndexWriter.write(s"${iindex}\t${iid}\n")
      numberOfItems = numberOfItems + 1
      // NOTE: keep u2i as Iterator[T], don't convert to List because u2i is large.
      val u2iActions = u2iDb.getAllByAppidAndIid(arg.appid, iid, sortedByUid=true)
        .filter( keepValidAction(_, arg.likeParam, arg.dislikeParam, arg.viewParam, arg.conversionParam) )
        .map( convertToRating(_, arg.likeParam, arg.dislikeParam, arg.viewParam, arg.conversionParam) )
        .reduceLeft{ (acc: U2IAction, cur: U2IAction) =>
          if (cur.uid == acc.uid) {
            // keep highest rating
            // TODO: support other conflict resolutions
            val curRating = cur.v.get
            val accRating = acc.v.get
            if (curRating > accRating) {
              cur
            } else {
              acc
            }
          } else {
            ratingsTempWriter.write(s"${acc.uid}\t${acc.iid}\t${acc.v.get}\n") // TOOD: remove
            val uindex = usersIndex(acc.uid)
            numberOfEntries = numberOfEntries + 1
            ratingsWriter.write(s"${uindex},${iindex},${acc.v.get}\n")
            cur
          }
        }
    }
    itemsIndexWriter.close()
    ratingsWriter.close()
    ratingsTempWriter.close()

    if (arg.matrixMarket) {
      /* write again with Matrix Market Format header */
      val mmWriter = new BufferedWriter(new FileWriter(new File(arg.outputDir+"ratings.mm"))) 
      mmWriter.write("%%MatrixMarket matrix coordinate real general\n")
      mmWriter.write(s"${usersIndex.size} ${numberOfItems} ${numberOfEntries}\n")
      val ratingsFile = Source.fromFile(arg.outputDir+"ratings.csv")
      ratingsFile.getLines().foreach { line =>
        val v = line.split(",")
        mmWriter.write(s"${v(0)} ${v(1)} ${v(2)}")
        mmWriter.newLine()
      }
      mmWriter.close()
    }
  }

  def keepValidAction(u2i: U2IAction, likeParam: Option[Int], dislikeParam: Option[Int],
    viewParam: Option[Int], conversionParam: Option[Int]): Boolean = {
    val keepThis: Boolean = u2i.action match {
      case ACTION_RATE => true
      case ACTION_LIKE => (likeParam != None)
      case ACTION_DISLIKE => (dislikeParam != None)
      case ACTION_VIEW => (viewParam != None)
      case ACTION_CONVERSION => (conversionParam != None)
      case _ => {
        assert(false, "Action type " + u2i.action + " in u2iActions appdata is not supported!")
        false // all other unsupported actions
      }
    }
    keepThis
  }

  def convertToRating(u2i: U2IAction, likeParam: Option[Int], dislikeParam: Option[Int],
    viewParam: Option[Int], conversionParam: Option[Int]) = {
    val convertedU2I: U2IAction = u2i.action match {
      case ACTION_RATE => u2i
      case ACTION_LIKE => likeParam.map( param => u2i.copy(v = Some(param)) ).getOrElse{
        assert(false, "Action type " + u2i.action + " should have been filtered out!")
        u2i
      }
      case ACTION_DISLIKE => dislikeParam.map( param => u2i.copy(v = Some(param)) ).getOrElse{
        assert(false, "Action type " + u2i.action + " should have been filtered out!")
        u2i
      }
      case ACTION_VIEW => viewParam.map( param => u2i.copy(v = Some(param)) ).getOrElse{
        assert(false, "Action type " + u2i.action + " should have been filtered out!")
        u2i
      }
      case ACTION_CONVERSION => conversionParam.map( param => u2i.copy(v = Some(param)) ).getOrElse{
        assert(false, "Action type " + u2i.action + " should have been filtered out!")
        u2i
      }
    }
    convertedU2I
  }

  def cleanup(arg: JobArg) = {

  }

}