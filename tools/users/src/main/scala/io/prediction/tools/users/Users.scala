package io.prediction.tools.users

import io.prediction.commons._
import jline.console._
import org.apache.commons.codec.digest.DigestUtils
import util.control.Breaks._

object Users {
  val config = new Config()
  val users = config.getSettingsUsers()

  private def md5password(password: String) = DigestUtils.md5Hex(password)

  def main(args: Array[String]) {
    if (!config.settingsDbConnectable()) {
      println(s"Cannot connect to ${config.settingsDbType}://${config.settingsDbHost}:${config.settingsDbPort}/${config.settingsDbName}. Aborting.")
      sys.exit(1)
    }
    println()
    println("PredictionIO CLI User Management")
    println()
    println("1 - Add a new user")
    println("2 - Update email of an existing user")
    println("3 - Change password of an existing user")
    val choice = readLine("Please enter a choice (1-3): ")
    choice match {
      case "1" => adduser()
      case "2" => updateEmail()
      case "3" => changePassword()
      case _ => println("Unknown choice")
    }
    println()
  }

  def adduser() = {
    val cr = new ConsoleReader()
    println("Adding a new user")
    val email = cr.readLine("Email: ")
    
    if (users.emailExists(email)) {
      println("Email already exists. Not adding.")
    } else {

      var password = ""
      var retyped = ""
      breakable {
        while (true) {
          password = cr.readLine("Password: ", new java.lang.Character('*'));
          retyped = cr.readLine("Retype password: ", new java.lang.Character('*'));
          if (password == retyped)
            break
          println("Passwords do not match. Please enter again.")
        }
      }

      val firstName = cr.readLine("First name: ")
      val lastName = cr.readLine("Last name: ")
    
      users.insert(
        email = email,
        password = md5password(password),
        firstname = firstName,
        lastname = lastName match {
          case "" => None
          case _ => Some(lastName)
        },
        confirm = email
      )
      users.confirm(email)
      println("User added")
    }
  }

  def updateEmail() = {
    val cr = new ConsoleReader()
    println("Updating email of an existing user")
    val email = cr.readLine("Current email: ")
    val password = cr.readLine("Password: ", new java.lang.Character('*'));
    users.authenticateByEmail(email, md5password(password)) map { id: Int =>
      val newEmail = cr.readLine("New email: ")
      if (users.emailExists(newEmail)) {
        println("New email already exists. Not updating.")
      } else {
        users.updateEmail(id, newEmail)
        println("Email updated.")
      }
    } getOrElse {
      println("Invalid email or password. Please try again.")
    }
  }

  def changePassword() = {
    val cr = new ConsoleReader()
    println("Changing password of an existing user")
    val email = cr.readLine("Email: ")
    val password = cr.readLine("Old password: ", new java.lang.Character('*'));
    users.authenticateByEmail(email, md5password(password)) map { id: Int =>
      var newPassword = ""
      var retyped = ""
      breakable {
        while (true) {
          newPassword = cr.readLine("New password: ", new java.lang.Character('*'));
          retyped = cr.readLine("Retype new password: ", new java.lang.Character('*'));
          if (newPassword == retyped)
            break
          println("New passwords do not match. Please enter again.")
        }
      }
      users.updatePassword(id, md5password(newPassword))
      println("Password updated.")
    } getOrElse {
      println("Invalid email or password. Please try again.")
    }
  }

}
