package model.database
import model.domain.{Account, Transaction}

import java.sql.{Connection, ResultSet, Statement}

class DB_PostgreS  (override val url: String) extends DB_Interface {

  /**
   * Connection for querying and updating DB through
   * TODO: need to actually use this instead of implicitly creating every time
   */
  override val conn: java.sql.Connection = createConn()

  /**
   * Starts up the DB with all necessary tables
   *
   * @param state: Statement, for interacting with DB
   * @return: Option[ResultSet] is just None since there's no return for creating
   */
  override def initDb (state: Statement): Option[ResultSet] = {
    var SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS SiteUser(
         |   userID            integer PRIMARY KEY NOT NULL
         |   )""".stripMargin

    conn.prepareStatement(SQL).executeUpdate()

    SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS Stock (
         |   name              VARCHAR(250) PRIMARY KEY NOT NULL,
         |   ticker            VARCHAR(250),
         |   market            VARCHAR(250)
         |   )""".stripMargin

    conn.prepareStatement(SQL).executeUpdate()

    SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS Account (
         |   accountID         VARCHAR(25) PRIMARY KEY NOT NULL,
         |   userID            integer REFERENCES SiteUser (UserID),
         |   name              VARCHAR(250)
         |   )""".stripMargin

    conn.prepareStatement(SQL).executeUpdate()

    SQL =
      s"""
         |CREATE TABLE IF NOT EXISTS Transaction (
         |  Transaction_ID    integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
         |  Order_Code        VARCHAR(9)  NOT NULL,
         |  TradeDate         DATE,
         |  SettleDate        DATE,
         |  Company           VARCHAR(250) NOT NULL,
         |  Action            VARCHAR(10)  NOT NULL,
         |  Quantity          DECIMAL DEFAULT 0,
         |  Price             DECIMAL DEFAULT 0,
         |  Commission        DECIMAL DEFAULT 0,
         |  Net               DECIMAL DEFAULT 0,
         |  Company_RAW       VARCHAR(250) NOT NULL,
         |  accountID         VARCHAR(25) REFERENCES Account (accountID)
         |  )""".stripMargin

    conn.prepareStatement(SQL).executeUpdate()
    None
  }

  /**
   * adds a new user into the DB, will do nothing if user already exists
   *
   * @param state: Statement, for interacting with DB
   * @param userID: String, the name of the user being added
   * @return: Option[ResultSet], None, since is not a query
   */
  override def addUser (state: Statement, userID: String): Option[ResultSet] = {
    val SQL =
      s"""
         |insert into SiteUser values ($userID)
         |on conflict(userID)
         |do nothing
         |""".stripMargin

    conn.prepareStatement(SQL).executeUpdate()
    None
  }

  /**
   * grabs all users from the database
   *
   * @param state: Statement. for interacting with DB
   * @return: Option[ResultSet], a set containing all users from DB
   */
  override def getUsers (state: Statement): Option[ResultSet] = {
    val SQL = "select * from SiteUser"
    Some(conn.prepareStatement(SQL).executeQuery())
  }

  /**
   * adds a new account to the DB, attributed to a user
   *
   * @param state     :Statement, for interacting with DB
   * @param accountID :String, name of account being added
   * @param userID    :String, name of user that this account is owned by
   * @return          :Option[ResultSet], none since is not query
   */
  override def addAcc (state: Statement, accountID: String, userID: String): Option[ResultSet]= {
    val SQL =
      s"""
         |insert into Account values ('$accountID', $userID)
         |on conflict (accountID)
         |do nothing
         |""".stripMargin

    conn.prepareStatement(SQL).executeUpdate()
    None
  }

  /**
   * Adds a new transaction into the DB
   *
   * @param state     :Statement, for interacting with DB
   * @param trade     :Transaction, transaction to be added
   * @param accountID :String, account that owns this transaction
   * @return          :Option[ResultSet],
   */
  override def addTxn (state: Statement, trade: Transaction, accountID: String): Option[ResultSet] = {
    val SQL = {
      s"""
         |INSERT INTO TRANSACTION VALUES (
         |  DEFAULT,
         |  '${trade.getID}',
         |  '${trade.getTrade.toSQL}',
         |  '${trade.getSettle.toSQL}',
         |  '${trade.getCompany.trim}',
         |  '${trade.getAction}',
         |  ${trade.getQuantity},
         |  ${trade.getPrice},
         |  ${trade.getCommission},
         |  ${trade.getNet},
         |  '${trade.getCompanyRAW.trim}',
         |  '$accountID'
         |);""".stripMargin
    }
    conn.prepareStatement(SQL).executeUpdate()
    None
  }

  override def getTxns (statement: Statement, accountID: String = ""): Option[ResultSet] = {
    var SQL = ""
    if (accountID.isEmpty) {
      SQL =
        s"""
           |select * from Transaction order by Transaction_ID
           |""".stripMargin
    } else {
      SQL =
        s"""
           |select * from Transaction
           |  where accountID='$accountID'
           |  order by Transaction_ID
           |""".stripMargin
    }

    if (SQL.isEmpty){
      throw new Error("something went wrong with the getTxns function")
      None
    }
    Option(statement.executeQuery(SQL))
  }

  override def getAccountNums (statement: Statement): Option[ResultSet] = {
    val SQL = "select * from Account order by userID"
    Some(statement.executeQuery(SQL))
  }

  override def getAccount
  (statement: Statement, accountID: String): Option[ResultSet] = {
    val SQL = s"select * from Account where accountID='$accountID'"
    Some(statement.executeQuery(SQL))
  }

  override def updateAccount
  (statement: Statement, account: Account)= {
    val SQL = s"update Account set name='${account.name}' where accountID='${account.accountID}'"
    conn.prepareStatement(SQL).executeUpdate()
    None
  }

  override def getReport (statement: Statement, accountID: String): Option[ResultSet] = {
    var SQL = ""
    if (accountID.isEmpty) {
      SQL =
        s"""
           |select count(transaction_ID), order_code, tradedate, action, accountID from Transaction
           |group by order_code,tradedate, action, accountID
           |order by tradedate, order_code, accountID
           |""".stripMargin

    } else {
      SQL =
        s"""
           |select count(transaction_ID), order_code, tradedate, action from Transaction
           |where accountID='$accountID'
           |group by order_code,tradedate, action
           |order by tradedate, order_code
           |""".stripMargin
    }

    Some(statement.executeQuery(SQL))
  }

  override def getDivTotals (statement: Statement, accountID: String): Option[ResultSet] = {
    val SQL =
      s"""
       |select extract (year from tradedate) as txn_year, extract(month from tradedate) as txn_month, sum(net) as monthly_sum
       |from transaction
       |where action='DIV' and accountID='$accountID'
       |group by txn_month, txn_year
       |""".stripMargin

    Some(statement.executeQuery(SQL))
  }

  override def averageReport  (statement: Statement, accountID: String): Option[ResultSet] = {
    val SQL = s"""
                   |select order_code, company, tradedate, sum(quantity) as amount from transaction
                   |    where accountID='$accountID' and order_code != ''
                   |    group by order_code, tradedate, company
                   |    order by tradedate, company
                   |""".stripMargin

    Some(statement.executeQuery(SQL))
  }

  override def allCompanies   (statement: Statement, accountID: String): Option[ResultSet] = {
    val SQL =
      s"""
         |select company from transaction
         |  where accountID='$accountID'
         |  group by company
         |""".stripMargin

    Some(statement.executeQuery(SQL))
  }

  override def getAccountsByUser (statement: Statement, userID: String): Option[ResultSet] = {
    val SQL = s"select * from Account where userID=$userID"
    Some(statement.executeQuery(SQL))
  }

  override def getTickers (statement: Statement): Option[ResultSet] = {
    val SQL = s"select * from Stock"
    Some(statement.executeQuery(SQL))
  }

  override def getSpecificTick (statement: Statement, stockName: String): Option[ResultSet] = {
    val SQL = s"select * from Stock where name='$stockName'"
    Some(statement.executeQuery(SQL))
  }

  override def addTicker (statement: Statement, name: String,
                          ticker: String, market: String): Option[ResultSet] = {
    val SQL =
      s"""
         |INSERT INTO Stock VALUES (
         |  '$name',
         |  '$ticker',
         |  '$market') on conflict do nothing
         |  ;""".stripMargin
    statement.executeUpdate(SQL)
    None
  }

  override def setTicker (statement: Statement, name: String,
                          ticker: String, market: String): Option[ResultSet] = {

    val SQL = s"update Stock set ticker='$ticker', market='$market' where name='$name'"

    statement.executeUpdate(SQL)
    None
  }

  override def getStockByAccount (statement: Statement, account: String, name: String): Option[ResultSet] = {

    val SQL = s"select * from Transaction where Company='$name' and accountID='$account' order by SettleDate"

    Some(statement.executeQuery(SQL))
  }
}