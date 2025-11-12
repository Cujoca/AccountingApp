package model.database

import model.domain.{Account, Transaction}
import org.postgresql.util.PSQLException

import java.sql.{DriverManager, ResultSet, SQLException, Statement}

trait DB_Interface {

  def url: String

  def conn: java.sql.Connection

  /**
   * Initiates a connection to the database
   *
   * @return java.sql.Connection - the connection object
   */
  def createConn (): java.sql.Connection = {
    try {
      val conn = DriverManager.getConnection (url)
      conn
    } catch {
      case e: SQLException => throw new RuntimeException(e)
    }
  }

  def close(): Unit = conn.close()

  def openState(): Statement = conn.createStatement ()

  def dropDB(state: Statement): Option[ResultSet] = {
    println("in dropdb")
    try {
      conn.prepareStatement("drop table if exists Transaction ").executeUpdate()
      println("deleted transaction")
      conn.prepareStatement("drop table if exists Account ").executeUpdate()
      println("deleted account")
      conn.prepareStatement("drop table if exists SiteUser ").executeUpdate()
      println("deleted siteuser")
      conn.prepareStatement("drop table if exists Stock").executeUpdate()
      println("deleted stocks")
    } catch {
      case e:PSQLException => println(e)
    }
    println("Database has been cleared")
    None
  }

  def initDb (state: Statement): Option[ResultSet]

  def addAcc (state: Statement, accountID: String, userID: String): Option[ResultSet]

  def addUser (state: Statement, userID: String): Option[ResultSet] = {
    val SQL =
      s"""
         |insert into SiteUser values ($userID)
         |on conflict(userID)
         |do nothing
         |""".stripMargin

    Option(state.executeQuery(SQL))
  }

  def addTxn (state: Statement, trade: Transaction, accountID: String): Option[ResultSet] = {

    val SQL = {
      s"""
         |INSERT INTO TRANSACTION VALUES (
         |  DEFAULT,
         |  '${trade.getID}',
         |  '${trade.getTrade.toSQL}',
         |  '${trade.getSettle.toSQL}',
         |  '${trade.getCompany}',
         |  '${trade.getAction}',
         |  ${trade.getQuantity},
         |  ${trade.getPrice},
         |  ${trade.getCommission},
         |  ${trade.getNet},
         |  '${trade.getCompanyRAW}',
         |  '$accountID'
         |);""".stripMargin
    }

    Option(state.executeQuery(SQL))
  }

  def getTxns         (state: Statement, accountID: String): Option[ResultSet]

  def getTxnsAll      (state: Statement): Option[ResultSet] =
    Option(state.executeQuery("select * from Transaction"))

  def getAccountNums  (state: Statement): Option[ResultSet]

  def getUsers        (state: Statement): Option[ResultSet]

  def getAccount      (state: Statement, accountID: String): Option[ResultSet]

  def updateAccount   (state: Statement, account: Account): Option[ResultSet]

  def getReport       (state: Statement, accountID: String): Option[ResultSet]

  def getDivTotals    (state: Statement, accountID: String): Option[ResultSet]

  def averageReport   (state: Statement, accountID: String): Option[ResultSet]

  def allCompanies    (state: Statement, accountID: String): Option[ResultSet]

  def getAccountsByUser (state: Statement, userID: String): Option[ResultSet]

  def getTickers      (state: Statement): Option[ResultSet]

  def addTicker       (state: Statement, name: String, ticker: String, market: String): Option[ResultSet]

  def getSpecificTick (state: Statement, stockName: String): Option[ResultSet]

  def setTicker       (state: Statement, name: String, ticker: String, market: String): Option[ResultSet]

  def getStockByAccount (state: Statement, account: String, name: String): Option[ResultSet]

  //etc etc

}
