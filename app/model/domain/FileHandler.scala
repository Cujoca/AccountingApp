package model.domain

import model.database.DB_Factory
import org.apache.commons.csv.CSVFormat

import java.io.{BufferedReader, FileReader, FileWriter}
import java.util
import java.util.Scanner
import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
 * Util library to handle transfers of data from config files
 */
object FileHandler {

  /**
   * Reads provided csv file and places transactions contained within into a HashSet for later use
   *
   * @param filePath String, location of the file to read
   * @return HashMap[String, model.domain.Report] the parsed transactions
   */
  def readFile (filePath: String, accountID: String, userID: String): mutable.HashMap[String, Report] = {

    val trades = new collection.mutable.HashSet[Transaction]

    val reader = new BufferedReader (new FileReader (filePath))
    reader.readLine ()
    reader.readLine ()
    reader.readLine ()
    reader.readLine ()
    val records = CSVFormat.DEFAULT.parse (reader)

    records.getRecords.forEach (record => {
      val inStr = record.toList
      val tran = model.domain.Transaction.TransFromCSV (inStr.asScala.toArray)
      trades add tran
    })

    reader.close ()

    trades foreach (trade => {
      if (trade.getCompanyRAW contains '\'') {
        trade.setCompanyRaw (trade.getCompanyRAW.replaceAll ("'", "&"))
        trade.setCompany (trade.getCompany.replaceAll ("'", "&"))
      }
    })

    sendToDB(trades, accountID, userID)
    tradesToView(trades)
  }

  /**
   * Takes the transaction set provided from readFile and directs the DB_PostgreS to
   * place the userID, accountID, and transactions into the online database
   *
   * @param trades    HashSet[model.domain.Transaction], set of transactions from readFile
   * @param accountID String, name of the account that these transactions are attributed to
   * @param userID    String, the user which this account is attributed to
   */
  private def sendToDB (trades: mutable.HashSet[Transaction], accountID: String, userID: String): Unit = {
    DB_Factory.withDB { (db, statement) =>
      db.initDb   (statement)
      db.addUser  (statement, userID)
      db.addAcc   (statement, accountID, userID)

      trades foreach (trade => {
        db.addTxn (statement, trade, accountID)
        db.addTicker(statement, trade.Company, "", "")
      })
      None
    }
  }

  /**
   * Takes the HashSet[Transactions] from fileReader and prepares them in a format expected
   * by the fileStatus view for the csv file report.
   *
   * @param trades  HashSet[model.domain.Transactions], the transactions from fileReader
   * @return        HashMap[String, model.domain.Report], the shares and associated data
   */
  private def tradesToView (trades: mutable.HashSet[Transaction]): mutable.HashMap[String, Report] = {

    val out = new scala.collection.mutable.HashMap[String, Report]

    trades foreach (tran => {

      val quant = tran.getQuantity

      if (!(out contains tran.getCompany)) {
        // all stats are set to 0 by default, will be changed in next section of code
        out put(tran.getCompany, new Report (0, 0, 0, 0, 0, 0))
      }
      tran.getAction match {
        case "BUY" =>
          out (tran.getCompany).++ ()
          out (tran.getCompany).+: (quant)

        case "SELL" =>
          out (tran.getCompany).-- ()
          out (tran.getCompany).-: (quant)

        case _ =>
          out (tran.getCompany).+- ()
          out (tran.getCompany).+: (quant)
      }
      out (tran.getCompany).addProfit (tran.getNet)
    })

    out

  }
}
