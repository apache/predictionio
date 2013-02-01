package io.prediction.tools.users

import io.prediction.commons.settings._
import jline.console._

object Users {
  val config = new Config()
  val users = config.getUsers()

  def main(args: Array[String]) {
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
    val password = cr.readLine("Password: ", new Character('*'));
    val firstName = cr.readLine("First name: ")
    val lastName = cr.readLine("Last name: ")
    if (users.emailExists(email)) {
      println("Email already exists. Not adding.")
    } else {
      users.insert(
        email = email,
        password = password,
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
