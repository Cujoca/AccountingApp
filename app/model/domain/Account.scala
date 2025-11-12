package model.domain

import model.database.{DB_Access, DB_Factory}
import play.api.mvc.PathBindable

case class Account (accountID: String, name: String, userID: String)
object Account {
  def update(account: Account ) = {
    DB_Factory.withDB((db, statement) => db.updateAccount(statement, account))
  }

  def getByID (accountID: String) = {
    val result = DB_Factory.withDB((db, statement) => db.getAccount(statement, accountID))
    if (result.isEmpty) throw new Error("something went wrong with getting the account")
    val account = result.get
    account.next
    Some(new Account(account.getString("accountID"), account.getString("name"), account.getString("userID")))
  }
}