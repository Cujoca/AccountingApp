package model.database

import model.domain.{Account, TransactionsDBHandler}

import java.sql.DriverManager

object DB_Access {
/*
  /**
   * Prepares and sends a string query to the database
   *
   * @param query String query
   * @return ResultSet
   */
  def select (query: String): java.sql.ResultSet = {
    //TODO: implement custom queries rather than use hardcoded query from below
    val sampleQuery = "select * from Transaction order by Transaction_ID"
    DB_Factory.withDB {(db, statement)=>
      statement.executeQuery(sampleQuery)
    }
  }

  /**
   * Grabs all transactions from the database and sends to the
   * TransactionsDBHandler for parsing and organizing.
   *
   * @return TransActionsDBHandler - object containing the info needed to generate list for client viewing
   */
  def loadIntoHandler (): model.domain.TransactionsDBHandler = {
    val handler = new TransactionsDBHandler
    handler.addFromSQL(this.select(""))
    handler
  }

  /**
   * grabs all users and accounts from the database and loads into a TransactionsDBHandler
   * to parse and organize info to be sent to the html page
   *
   * @return Map[String, List[(String, String)] map containing:
   *         userID:        String - id of the user who owns these accounts
   *         List[
   *            accountID:  String - id of the account
   *            name:       String - nickname for the account
   *            ]
   */
  def getAccountNums (): Map[String, List[(String, String)]] = {
    val handler = new TransactionsDBHandler
    val query = "select * from Account order by userID"
    val result = DB_Connect.initConn().createStatement().executeQuery(query)
    handler.returnAccounts(result)
  }

  /**
   * uh, I don't really know what this does
   * from what I can tell, it's just returning if the account exists?
   *
   * @param accountID: String, account to search for
   * @return Option[Account], option containing account if exists
   */
  def getAccount (accountID: String): Option[Account] = {
    val handler = new TransactionsDBHandler
    val query = s"select * from Account where accountID='$accountID'"
    val result = DB_Connect.initConn().createStatement().executeQuery(query)
    if (result.next()) {
      Some(new Account (result.getString ("accountID"),
        result.getString ("name"), result.getString ("userID")))
    } else {
      None
    }
  }

  /**
   *
   *
   * @param account
   */
  def updateAccount (account: Account): Unit = {
    val query = s"update Account set name='${account.name}' where accountID='${account.accountID}'"
    DB_Connect.initConn().createStatement().executeUpdate(query)
  }

  /**
   * grab sql data necessary to generate a report for a specified accountID
   *
   * @param accountID: String, id for the account
   * @return ResultSet, sql data from the database
   */
  def getReport (accountID: String) = {
    val query =
      s"""
         |select count(transaction_ID), order_code, tradedate, action from Transaction
         |where accountID='$accountID'
         |group by order_code,tradedate, action
         |order by tradedate, order_code
         |""".stripMargin

    DB_Connect.initConn().createStatement().executeQuery(query)
  }

  /**
   * grabs the total amount of div returns by year/month for an account
   *
   * @param accountID: String, id of the account to get data about
   * @return ResultSet, data returned from database
   */
  def getDivTotals (accountID: String) = {
    val query = s"""
       |select extract (year from tradedate) as txn_year, extract(month from tradedate) as txn_month, sum(net) as monthly_sum
       |from transaction
       |where action='DIV' and accountID='$accountID'
       |group by txn_month, txn_year
       |""".stripMargin

    DB_Connect.initConn().createStatement().executeQuery(query)
  }

  /**
   * grabs the data necessary to generate a report for
   * the average holding time of each share associated with an account
   *
   * @param accountID: String, id of the account to generate the report
   * @return ResultSet, the data from the database
   */
  def averageReport (accountID: String) = {

    val query = s"""
       |select order_code, company, tradedate, sum(quantity) as amount from transaction
       |    where accountID='$accountID' and order_code != ''
       |    group by order_code, tradedate, company
       |    order by tradedate, company
       |""".stripMargin

    DB_Connect.initConn().createStatement().executeQuery(query)
  }

  def allCompanies (accountID: String) = {
    val query =
      s"""
         |select company from transaction
         |  where accountID='$accountID'
         |  group by company
         |""".stripMargin

    DB_Connect.initConn().createStatement().executeQuery(query)
  }
 */
}