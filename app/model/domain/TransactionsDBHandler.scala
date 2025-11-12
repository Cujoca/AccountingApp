package model.domain

import model.database.DB_Factory

import java.sql.ResultSet
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Class to hold all transactions, will also provide access to these transactions
 * TODO: figure out how to make this more private
 */
class TransactionsDBHandler {

  val transactions = new mutable.HashMap[Int, Transaction]
  val companies = new mutable.HashSet[String]
  val accounts = new mutable.HashSet[String]
  var tickerData = new mutable.HashMap[String, (String, String)]()


  def getFromSQL (): Unit = {
    val result = DB_Factory.withDB((db, state) => {
      db.getTxnsAll(state)
    })

    if (result.isEmpty) throw new Error("something went wrong getting stuff from database")

    addFromSQL(result.get)
  }

  /**
   * Takes an SQL query result and grabs each transaction from the result
   * places all transactions into the transactions hashmap, with the DB generated
   * transaction_id primary key as the hashmap key
   *
   * @param query: ResultSet - the result from the SQL query
   */
  def addFromSQL (query: java.sql.ResultSet): Unit = {

    transactions.clear()

    while (query.next()) {
      val company = query.getString ("Company")

      val addition = new Transaction (query.getString ("Order_Code"),
        Date.fromSQL (query.getString ("SettleDate")),
        Date.fromSQL (query.getString ("TradeDate")),
        company,
        query.getString ("Action"),
        query.getDouble ("Quantity"),
        query.getDouble ("Price"),
        query.getDouble ("commission"),
        query.getDouble ("Net"),
        query.getString ("Company_RAW"),
        query.getString ("accountID"))

      val additionID = query.getInt ("Transaction_ID")
      transactions put(additionID, addition)

      if (!(companies contains company)) companies add company
    }
  }

  /**
   * returns transactions map values as list
   *
   * @return List[Transaction]
   */
  def toList: List[Transaction] = {
    this.transactions.values.toList
  }

  /**
   * adds a new account into the account set
   *
   * @param account new account number: String
   */
  def addAccount (account: String): Unit = {
    accounts add account
  }

  def getAccount (accountID: String) = {
    val result = DB_Factory.withDB((db, statement) => db.getAccount(statement, accountID))
    if (result.isEmpty) throw new Error("something went wrong getting an account")
    val account = result.get
    account.next

    Some(new Account (account.getString ("accountID"),
      account.getString ("name"), account.getString ("userID")))
  }

  def getAccountsByUser: Map[String, List[(String, String, Report)]] = {
    var result = DB_Factory.withDB((db, statement) => {db.getAccountNums(statement)})
    if (result.isEmpty) throw new Error("something went wrong getting accounts")
    val accounts = returnAccounts(result.get)

    result = DB_Factory.withDB((db, statement) => {
      db.getReport(statement, "")
    })

    if (result.isEmpty) throw new Error("something went wrong while getting reports for all accounts")
    val reportMap = Report.parseReportAll()

    val out = new mutable.HashMap[String, List[(String, String, Report)]]
    var temp = new ListBuffer[(String, String, Report)]

    accounts foreach (account => {
      temp.clear
      account._2 map (entry => temp.append((entry._1, entry._2, reportMap(entry._1))))
      out put (account._1, temp.toList)
    })

    out.toMap
  }

  /**
   * loads unique account numbers into a list
   *
   * @return List[String]
   */
  def returnAccounts (result: ResultSet): Map[String, List[(String, String)]] = {
    val outMap = new mutable.HashMap[String, List[(String, String)]]
    var out = new ListBuffer[(String, String)]
    while (result.next()) {
      if (outMap.keySet.contains(result.getString("userID"))) {
        out.append((result.getString("accountID"), result.getString("name")))
        outMap put (result.getString("userID"), out.toList)
      } else {
        out = new ListBuffer[(String, String)]()
        out append ((result.getString("accountID"), result.getString("name")))
        outMap put (result.getString("userID"), out.toList)
      }
    }
    outMap.toMap
  }

  def addTxn (txn: Transaction) = {
    DB_Factory.withDB((db, statement) => db.addTxn(statement, txn, txn.getAccountNum))
  }

  def addTicker (name: String, ticker: String, market: String) = {
    DB_Factory.withDB((db, statement) => db.setTicker(statement, name, ticker, market))
  }

  def getTickers () = {
    val resultSet = DB_Factory.withDB((db, statement) => db.getTickers(statement))

    if (resultSet.isEmpty) throw new Error("error getting tickers from DB")

    val result = resultSet.get

    while (result.next) {
      tickerData.put(result.getString("name"), (result.getString("ticker"), result.getString("market")))
    }

    tickerData = tickerData.filter(elem => "[0-9]".r().findAllIn(elem._1).isEmpty)
  }

  def getStockDetail (account: String, name: String): List[(String, Double, Double)] = {
    val resultSet = DB_Factory.withDB((db, statement) => db.getStockByAccount(statement, account, name))

    if (resultSet.isEmpty) throw new Error("error getting transactions")

    Report.getStockDetails(resultSet.get)
  }
}


