package model.domain

import model.database.DB_Factory

import scala.collection.mutable

object Users {

  def grabUsers: mutable.HashSet[String] = {
    val resultOP = DB_Factory.withDB((db, statement) => db.getUsers(statement))
    if (resultOP.isEmpty) throw new Error("something went wrong getting users")

    val result = resultOP.get

    val out = new mutable.HashSet[String]

    while (result.next()) out add (result.getString("userID"))

    out
  }

  def getAccountsByUser (userID: String): mutable.HashSet[(String, String)] = {
    val resultOP = DB_Factory.withDB((db, statement) => db.getAccountsByUser(statement, userID))

    if (resultOP.isEmpty) throw new Error("something went wrong gettin accounts by user")

    val out = new mutable.HashSet[(String, String)]
    val result = resultOP.get

    while (result.next()) out add ((result.getString("accountID"), result.getString("name")))

    out
  }

}
