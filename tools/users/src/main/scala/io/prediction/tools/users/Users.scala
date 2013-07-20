package io.prediction.tools.users

import io.prediction.commons._
import jline.console._
import org.apache.commons.codec.digest.DigestUtils

object Users {
  val config = new Config()
  val users = config.getSettingsUsers()

  private def md5password(password: String) = DigestUtils.md5Hex(password)

  def main(args: Array[String]) {
    if (!config.settingsDbConnectable()) {
      println(s"Cannot connect to ${config.settingsDbType}://${config.settingsDbHost}:${config.settingsDbPort}/${config.settingsDbName}. Aborting.")
      sys.exit(1)
    }
    println("PredictionIO CLI User Management")
    println("1. Add a confirmed user")
    val choice = readLine("Enter a choice: ")
    choice match {
      case "1" => adduser()
      case _ => println("Unknown choice")
    }
  }

  def adduser() = {
    val cr = new ConsoleReader()
    println("Adding a confirmed user")
    val email = cr.readLine("Email: ")
    val password = cr.readLine("Password: ", new java.lang.Character('*'));
    val firstName = cr.readLine("First name: ")
    val lastName = cr.readLine("Last name: ")
    if (users.emailExists(email)) {
      println("Email already exists. Not adding.")
    } else {
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
}
