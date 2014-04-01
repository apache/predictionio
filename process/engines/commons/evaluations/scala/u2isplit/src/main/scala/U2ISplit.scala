package io.prediction.evaluations.commons.u2isplit

import io.prediction.commons.Config
import io.prediction.commons.appdata.{ Item, Items, U2IAction, U2IActions, User, Users }
import io.prediction.commons.filepath.{ U2ITrainingTestSplitFile }

import java.io.{ BufferedWriter, File, FileWriter }
import scala.io.Source

import com.github.nscala_time.time.Imports._
import grizzled.slf4j.Logger
import org.json4s.native.Serialization

case class U2ISplitConfig(
  sequenceNum: Int = 0,
  appid: Int = 0,
  engineid: Int = 0,
  evalid: Int = 0,
  itypes: Option[Seq[String]] = None,
  trainingpercent: Double = 0,
  validationpercent: Double = 0,
  testpercent: Double = 0,
  timeorder: Boolean = false)

/**
 * User-to-Item Action Splitter for Single Machine
 *
 * TODO: Eliminate use of Config object. Let scheduler handles it all.
 */
object U2ISplit {
  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[U2ISplitConfig]("u2isplit") {
      head("u2isplit")
      opt[Int]("sequenceNum") required () action { (x, c) =>
        c.copy(sequenceNum = x)
      } text ("the sequence number (starts from 1 for the 1st iteration and then increment for later iterations)")
      opt[Int]("appid") required () action { (x, c) =>
        c.copy(appid = x)
      } text ("the App ID to split data from")
      opt[Int]("engineid") required () action { (x, c) =>
        c.copy(engineid = x)
      } text ("the Engine ID to split data to")
      opt[Int]("evalid") required () action { (x, c) =>
        c.copy(evalid = x)
      } text ("the OfflineEval ID to split data to")
      opt[String]("itypes") action { (x, c) =>
        c.copy(itypes = Some(x.split(',')))
      } text ("restrict use of certain itypes (comma-separated, e.g. --itypes type1,type2)")
      opt[Double]("trainingpercent") required () action { (x, c) =>
        c.copy(trainingpercent = x)
      } validate { x =>
        if (x >= 0.01 && x <= 1) success else failure("--trainingpercent must be between 0.01 and 1")
      } text ("size of training set (0.01 to 1)")
      opt[Double]("validationpercent") required () action { (x, c) =>
        c.copy(validationpercent = x)
      } validate { x =>
        if (x >= 0 && x <= 1) success else failure("--validationpercent must be between 0 and 1")
      } text ("size of validation set (0 to 1)")
      opt[Double]("testpercent") required () action { (x, c) =>
        c.copy(testpercent = x)
      } validate { x =>
        if (x >= 0.01 && x <= 1) success else failure("--testpercent must be between 0.01 and 1")
      } text ("size of test set (0.01 to 1)")
      opt[Boolean]("timeorder") action { (x, c) =>
        c.copy(timeorder = x)
      } text ("set to true to sort the sampled results in time order before splitting (default to false)")
      checkConfig { c =>
        if (c.trainingpercent + c.validationpercent + c.testpercent > 1) failure("sum of training, validation, and test sizes must not exceed 1") else success
      }
    }

    parser.parse(args, U2ISplitConfig()) map { config =>
      val logger = Logger(U2ISplit.getClass)
      val commonsConfig = new Config()
      val usersFilePath = U2ITrainingTestSplitFile(
        rootDir = commonsConfig.settingsLocalTempRoot,
        appId = config.appid,
        engineId = config.engineid,
        evalId = config.evalid,
        name = "users")
      val usersFile = new File(usersFilePath)
      val itemsFilePath = U2ITrainingTestSplitFile(
        rootDir = commonsConfig.settingsLocalTempRoot,
        appId = config.appid,
        engineId = config.engineid,
        evalId = config.evalid,
        name = "items")
      val itemsFile = new File(itemsFilePath)
      val u2iActionsFilePath = U2ITrainingTestSplitFile(
        rootDir = commonsConfig.settingsLocalTempRoot,
        appId = config.appid,
        engineId = config.engineid,
        evalId = config.evalid,
        name = "u2iActions")
      val u2iActionsFile = new File(u2iActionsFilePath)
      implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

      // If this is the first iteration (sequence), take a snapshot of appdata
      if (config.sequenceNum == 1) {
        logger.info("This is the first iteration. Taking snapshot of app's data...")

        val usersDb = commonsConfig.getAppdataUsers
        val itemsDb = commonsConfig.getAppdataItems
        val u2iDb = commonsConfig.getAppdataU2IActions

        // Create the output directory if does not yet exist
        val outputDir = new File(U2ITrainingTestSplitFile(
          rootDir = commonsConfig.settingsLocalTempRoot,
          appId = config.appid,
          engineId = config.engineid,
          evalId = config.evalid,
          name = ""))
        outputDir.mkdirs()

        // Dump all users and fix ID prefices
        logger.info(s"Writing to: $usersFilePath")
        val usersWriter = new BufferedWriter(new FileWriter(usersFile))
        usersDb.getByAppid(config.appid) foreach { user =>
          usersWriter.write(Serialization.write(user.copy(appid = config.evalid)))
          usersWriter.newLine()
        }
        usersWriter.close()

        // Dump all items and fix ID prefices
        // Filtered by itypes
        logger.info(s"Writing to: $itemsFilePath")
        val itemsWriter = new BufferedWriter(new FileWriter(itemsFile))
        val validIids = collection.mutable.Set[String]()
        config.itypes map { t =>
          val engineItypes = t.toSet
          itemsDb.getByAppid(config.appid) foreach { item =>
            if (item.itypes.toSet.intersect(engineItypes).size > 0) {
              itemsWriter.write(Serialization.write(item.copy(appid = config.evalid)))
              itemsWriter.newLine()
              validIids += item.id
            }
          }
        } getOrElse {
          itemsDb.getByAppid(config.appid) foreach { item =>
            itemsWriter.write(Serialization.write(item.copy(appid = config.evalid)))
            itemsWriter.newLine()
            validIids += item.id
          }
        }
        itemsWriter.close()

        // Dump all actions and fix ID prefices
        // Filtered by itypes
        logger.info(s"Writing to: $u2iActionsFilePath")
        var u2iCount = 0
        val u2iActionsWriter = new BufferedWriter(new FileWriter(u2iActionsFile))
        u2iDb.getAllByAppid(config.appid) foreach { u2iAction =>
          if (validIids(u2iAction.iid)) {
            u2iActionsWriter.write(Serialization.write(u2iAction.copy(appid = config.evalid)))
            u2iActionsWriter.newLine()
            u2iCount += 1
          }
        }
        u2iActionsWriter.close()

        // Save the count of U2I actions
        val u2iActionsCountWriter = new BufferedWriter(new FileWriter(new File(u2iActionsFilePath + "Count")))
        u2iActionsCountWriter.write(u2iCount.toString)
        u2iActionsCountWriter.close()
      }

      // Read snapshots
      logger.info("Reading snapshots...")

      val trainingUsersDb = commonsConfig.getAppdataTrainingUsers
      val trainingItemsDb = commonsConfig.getAppdataTrainingItems
      val trainingU2iDb = commonsConfig.getAppdataTrainingU2IActions
      val validationU2iDb = commonsConfig.getAppdataValidationU2IActions
      val testU2iDb = commonsConfig.getAppdataTestU2IActions

      val totalCount = Source.fromFile(new File(u2iActionsFilePath + "Count")).mkString.toInt
      val evaluationCount = (math.floor((config.trainingpercent + config.validationpercent + config.testpercent) * totalCount)).toInt
      val trainingCount = (math.floor(config.trainingpercent * totalCount)).toInt
      val validationCount = (math.floor(config.validationpercent * totalCount)).toInt
      val trainingValidationCount = trainingCount + validationCount
      val testCount = evaluationCount - trainingValidationCount

      logger.info(s"Reading from: $usersFilePath")
      trainingUsersDb.deleteByAppid(config.evalid)
      Source.fromFile(usersFile).getLines() foreach { userJson =>
        trainingUsersDb.insert(Serialization.read[User](userJson))
      }

      logger.info(s"Reading from: $itemsFilePath")
      trainingItemsDb.deleteByAppid(config.evalid)
      Source.fromFile(itemsFile).getLines() foreach { itemJson =>
        trainingItemsDb.insert(Serialization.read[Item](itemJson))
      }

      /**
       * Perform itypes filtering at this point because itypes is an
       * engine-specific parameter, and we want the split percentage to
       * be relative to the total number of items that is valid for this
       * particular engine.
       */
      logger.info(s"Reading from: $u2iActionsFilePath")
      trainingU2iDb.deleteByAppid(config.evalid)
      validationU2iDb.deleteByAppid(config.evalid)
      testU2iDb.deleteByAppid(config.evalid)
      val allU2iActions = Source.fromFile(u2iActionsFile).getLines().map(Serialization.read[U2IAction](_))
      val unsortedEvalU2iActions = util.Random.shuffle(allU2iActions).take(evaluationCount)
      val evalU2iActions = if (config.timeorder) unsortedEvalU2iActions.toSeq.sortWith(_.t + 0.seconds < _.t + 0.seconds) else unsortedEvalU2iActions.toSeq
      var count = 0
      evalU2iActions foreach { u2iAction =>
        if (count < trainingCount)
          trainingU2iDb.insert(u2iAction)
        else if (count >= trainingCount && count < trainingValidationCount)
          validationU2iDb.insert(u2iAction)
        else
          testU2iDb.insert(u2iAction)
        count += 1
      }
    }
  }
}
