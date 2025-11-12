package model.domain

import java.util
import scala.util.control.Breaks.break

/**
 * Class to represent a transaction taken by the client
 * 
 * @param ID          ID, resets per month
 * @param TradeDate   date the trade occurred
 * @param SettleDate  date the trade was settled
 * @param Company     stock that trade consists of
 * @param Action      action of the trade (Sell, Buy, Div)
 * @param Quantity    how much stock was traded
 * @param Price       price per stock
 * @param Commission  commission for trader (bank in this case)
 * @param Net         net cost of transaction
 * @param Company_RAW raw string from the csv file
 */
class Transaction (ID: String,
                   TradeDate: Date, 
                   SettleDate: Date,
                   var Company: String,
                   Action: String,
                   Quantity: Double,
                   Price: Double,
                   Commission: Double,
                   Net: Double,
                   var Company_RAW: String,
                   accountNum: String = "") {

  /**
   * Represent a transaction as an array of strings for internal use
   * the returned array will have the form:
   * ID, TradeDate, SettleDate, Company, Action, Price, Commission, Net Amount
   *
   * @return the formatted array
   */
  def toStringArray: Array[String] = {
    val out = new Array[String](4)
    out :+ ID
    out :+ TradeDate.toString
    out :+ SettleDate.toString
    out :+ Company
    out :+ Action
    out :+ Quantity+""
    out :+ Price+""
    out :+ Commission+""
    out :+ Net+""
    out :+ accountNum
    out
  }

  override def toString: String = {
    s"""
       |$ID, ${TradeDate.toString}, ${SettleDate.toString}, $Company, $Action, $Quantity, $Price, $Commission, $Net
       |""".stripMargin
  }

  def getID: String = this.ID
  def getCompany: String = this.Company
  def getTrade: Date = this.TradeDate
  def getSettle: Date = this.SettleDate
  def getAction: String = this.Action
  def getPrice: Double = this.Price
  def getCommission: Double  = this.Commission
  def getQuantity: Double  = this.Quantity
  def getNet: Double = this.Net
  def getCompanyRAW: String = this.Company_RAW
  def getAccountNum: String = this.accountNum
  def setCompanyRaw (add: String): Unit = this.Company_RAW = add
  def setCompany(add: String): Unit = this.Company= add
}

/**
 * Companion object for Transaction, contains functions to decode individual transactions from
 * provided csv file
 */
object Transaction {

  /**
   * Creates new transaction instance from one line in a csv file
   *
   * @param args set of args from the csv line in the form <br>
   *             TradeDate, SettleDate, Description(company), Action, Quantity, Price, Commission, Net
   * @return new transaction instance
   */
  def TransFromCSV(args: Array[String]): Transaction = {
    val tradeDate = Date.fromString(args(0))
    val settleDate = Date.fromString(args(1))
    val (company, code, raw) = simplifyCompany(args(2))

    new Transaction(code, tradeDate, settleDate,
      company.trim, args(3).toUpperCase,
      simplifyDouble(args(4)), simplifyDouble(args(5)), simplifyDouble(args(6)), simplifyDouble(args(7)), raw.trim)
  }

  /**
   * Takes the company string from the csv file and separates the company name and transaction code
   * will remove -NEW if exists in the company name
   *
   * @param arg company string
   * @return cleaned up name and order number if any
   */
  private def simplifyCompany(arg: String): (String,String,String) = {
    val codeRegex = "(.*)([A-Z]{2}-[0-9]{6})$"
    val codeNew = "-NEW"

    var code = ""
    var company = ""

    if (arg matches codeRegex) {
      code = arg slice (arg.length-9, arg.length-1)
      company = arg slice (0, arg.length-10)
    } else {
      company = arg
    }
    company = company replaceAll ("-NEW$", " ")
    (company, code, arg)
  }

  /**
   * takes a string representation for some double number, and changes it into a double
   * in the csv file, this can take form as either a normal double, or scientific notation
   * EG: xE+6
   * 
   * @param arg
   * @return
   */
  private def simplifyDouble (arg: String): Double = {
    if (arg equals "") return 0

    if (arg contains "E+") {
      val out = arg.slice(0, arg.length-3).toDouble
      val mult = (arg(arg.length-1)+"").toDouble
      return out * Math.pow(10, mult)
    }
    arg.toDouble
  }
}
