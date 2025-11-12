package model.database

import model.domain.Report
import org.apache.commons.csv.{CSVFormat, CSVParser}

import java.io.{BufferedReader, FileReader}
import java.sql.{DriverManager, Statement}
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.language.postfixOps

object DB_StartUp {

  /**
   * does quite a lot of things, i'll try to go over them here
   *
   *    creates tables for data, and populates them
   *    with the starting data given by the user from the uploaded file
   *    including the:
   *      - SiteUser - not really used yet, but will be to ensure that
   *        users only get to access data and accounts that belong to them
   *
   *      - Account  - to attribute certain trades and shares to specific accounts
   *        which will then be used to group said transactions together later for reports
   *
   *      - Transaction - The big one, is the table that will contain all transactions parsed
   *        from the provided file which will then be put into the DB from this function.
   *        TODO: Should probably do this within a domain class to proper mvc design
   *
   *
   * @param fileName:   String, the file from which stuff will be parsed
   * @param accountNum: String, the account which is taken and filled automatically from the filename
   * @param userID:     String, the user with which this account and set of transactions is allocated to
   * @return HashMap[String, Report], a hashmap containing a report for each company contained within the transactionset
   */
  def startTable (fileName: String, accountNum: String, userID: String) = {
    var conn = DB_Connect.initConn ()

    var SQL =
      s"""
        |CREATE TABLE IF NOT EXISTS SiteUser(
        |   userID            integer PRIMARY KEY NOT NULL
        |   )""".stripMargin

    var res = conn.prepareStatement(SQL).executeUpdate()

    SQL =
      s"""
         |insert into SiteUser values ($userID)
         |on conflict(userID)
         |do nothing
         |""".stripMargin

    res = conn.prepareStatement(SQL).executeUpdate()

    SQL =
      s"""
        |CREATE TABLE IF NOT EXISTS Account (
        |   accountID         VARCHAR(25) PRIMARY KEY NOT NULL,
        |   userID            integer REFERENCES SiteUser (UserID),
        |   name              VARCHAR(250)
        |   )""".stripMargin

    res = conn.prepareStatement(SQL).executeUpdate()

    SQL =
      s"""
         |insert into Account values ('$accountNum', $userID)
         |on conflict (accountID)
         |do nothing
         |""".stripMargin

    res = conn.prepareStatement(SQL).executeUpdate()

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


    res = conn.prepareStatement (SQL).executeUpdate ()

    val trades = new collection.mutable.HashSet[model.domain.Transaction]

    val reader = new BufferedReader(new FileReader(fileName))
    reader.readLine ()
    reader.readLine ()
    reader.readLine ()
    reader.readLine ()
    val records = CSVFormat.DEFAULT.parse(reader)

    records.getRecords.forEach(record => {
      val inStr = record.toList
      println(inStr)
      val tran = model.domain.Transaction.TransFromCSV (inStr.asScala.toArray)
      trades add tran
    })

    reader.close()



    trades foreach (trade => {
      if (trade.getCompanyRAW contains '\'') {
        trade.setCompanyRaw(trade.getCompanyRAW.replaceAll("'", "&"))
        trade.setCompany(trade.getCompany.replaceAll("'", "&"))
      }
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
           |  '$accountNum'
           |);""".stripMargin
      }
      val ps = conn.prepareStatement (SQL)
      ps.executeUpdate ()


    })

    val out = new scala.collection.mutable.HashMap[String, Report]

    trades foreach (tran => {

      val quant = tran.getQuantity

      if (!(out contains tran.getCompany)) {
        // all stats are set to 0 by default, will be changed in next section of code
        out put (tran.getCompany, new Report(0, 0, 0, 0, 0, 0))
      }
      tran.getAction match {
        case "BUY" =>
          out(tran.getCompany).++()
          out(tran.getCompany).+:(quant)

        case "SELL" =>
          out(tran.getCompany).--()
          out(tran.getCompany).-:(quant)

        case _ =>
          out(tran.getCompany).+-()
          out(tran.getCompany).+:(quant)
      }
      out(tran.getCompany).addProfit(tran.getNet)
    })

    out
  }
}
