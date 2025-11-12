package model.domain

import model.database.{DB_Access, DB_Factory}
import sttp.client4.Response
import sttp.client4.quick._
import sttp.model.Uri
import com.alibaba.fastjson
import com.alibaba.fastjson.JSON

import java.io.{BufferedReader, InputStreamReader}
import java.net.{HttpURLConnection, URI, URL, URLConnection}
import java.sql.ResultSet
import java.util.Calendar
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.NoSourceFile.content

/**
 * class for parsing through results from sql queries
 * and bundling relevant data to present to user
 *
 * @param buyOrder    int number of buy orders in report
 * @param sellOrder   int number of sell orders in report
 * @param otherOrder  int number of other orders in report
 * @param totalBought double number of total shares bought
 * @param totalSold   double number of total shares sold
 * @param profit      double net result of trade
 */
case class Report (var buyOrder: Int,
              var sellOrder: Int,
              var otherOrder: Int,
              var totalBought: BigDecimal,
              var totalSold: BigDecimal,
              var profit: BigDecimal) {

  def addProfit (add: BigDecimal): Unit = {
    this.profit = this.profit+add
  }

  /**
   * increments buy order
   */
  def ++ (): Unit = {
    this.buyOrder+=1
  }

  /**
   * increments sell order
   */
  def -- (): Unit  = {
    this.sellOrder+=1
  }

  /**
   * increments other order
   */
  def +- (): Unit = {
    this.otherOrder+=1
  }

  /**
   * increases shares bought from trade
   * @param buy double number of shares bought
   */
  def +: (buy: BigDecimal): Unit = {
    this.totalBought+=buy
  }

  /**
   * increases shares sold from trade
   * @param sell double number of shares sold
   */
  def -: (sell: BigDecimal): Unit = {
    this.totalSold+=sell
  }

  /**
   * gets net share increase at end of report
   * @return double number of shares
   */
  def getNet: BigDecimal = {
    this.totalBought+this.totalSold
  }
}

/**
 * companion object for report containing functions to parse through data and build report
 */
object Report {

  /**
   * returns report containing relevant info for each month
   *
   * @param result ResultSet from sql query
   * @return HashMap[String, Report] hashmap of data (year/month -> data)
   */
  def parseReport(result: ResultSet): mutable.HashMap[String, Report] = {
    val out = new mutable.HashMap[String, Report]
    while (result.next()) {
      val date = Date.fromSQL(result.getString("tradedate")).toStringMMYY
      if (!out.contains(date)) {
        out put (date, new Report(0,0,0,0,0,0))
      }
      val temp = out(date)
      result.getString("action") match {
        case "BUY" => temp.++()
        case "SELL" => temp.--()
        case _ =>
      }
      out put (date, temp)
    }
    out
  }

  def parseReportList (accounts: mutable.HashSet[(String, String)]) = {
    val out = new mutable.HashMap[(String, String), Report]

    accounts foreach (account => {
      println(s"checking account $account")
      val result = DB_Factory.withDB((db, statement) => db.getReport(statement, account._1)).get
      val dates = parseReport(result)

      val latestMonth = dates(dates.keySet.max)

      println(latestMonth)

      out put ((account._1, account._2), latestMonth)
    })
    out
  }

  def parseReportAll () = {
    val accounts = new ListBuffer[String]

    val out = new mutable.HashMap[String, Report]

    var result = DB_Factory.withDB((db, statement) => db.getAccountNums(statement)).get
    while (result.next) accounts.append(result.getString("accountID"))

    accounts foreach (account => {
      result = DB_Factory.withDB((db, statement) => db.getReport(statement, account)).get
      val dates = parseReport(result)

      val latestMonth = dates(dates.keySet.max)

      out put (account, latestMonth)
    })
    out
  }

  /**
   * adds total dividend returns to report, uses profit field to store
   *
   * @param result                ResultSet from sql query
   * @param out                   map of data (year/month -> data)
   * @return Map[String, Report]  calculated sum of dividend returns per share
   */
  def addDivToReport (accountID: String) = {
    val query = DB_Factory.withDB((db, statement) => {
      db.getReport(statement, accountID)
    })
    val out = parseReport(query.get)

    val result = DB_Factory.withDB((db, statement) => db.getDivTotals(statement, accountID)).get


    while (result.next()) {
      val date = result.getString("txn_year") + "/" + result.getString("txn_month")
      val temp = out(date)
      temp.addProfit(result.getBigDecimal("monthly_sum"))
      out put (date, temp)
    }
    out.toMap
  }


  private case class Trade (var amount: Double, date: Date)
  
  def averageReport (accountID: String): List[(String, Int, Boolean)] = {
    val result = DB_Factory.withDB((db, statement) => db.averageReport(statement, accountID))
    if (result.isEmpty) throw new Error("something wrong with average report")
    averageHoldingTime(result.get)
  }

  /**
   * Generates a report that gives the average holding time of all shares associated
   * with a specific account. All time differences are calculated on a share by share basis.
   *
   * Any shares that have more sold than are currently owned will tally up the total needed
   * to be provided to the user on the web page. Any share that is NOT sold and still held
   * by the end of the report will be assumed to be sold today so to be used in the average calculation
   *
   * @param result ResultSet from sql query
   * @return List[(String, Int, Boolean)] - a list of results per share which contain the
   *         name:    String              - name of the share
   *         average: Int                 - average holding period of each share in days
   *                                        or the number of shares oversold
   *                                        (truncated to int due to request)
   *         status:  Boolean              - true if share is balanced sufficiently, false if not
   *
   */
  private def averageHoldingTime (result: ResultSet): List[(String, Int, Boolean)] = {
    val companies = new mutable.HashMap[String, ListBuffer[Trade]]
    var temp = new ListBuffer[Trade]

    while (result.next()) {
      if (companies contains (result.getString("company"))) {
        temp = companies(result.getString("company"))
        temp append Trade(result.getDouble("amount"), Date.fromSQL(result.getString("tradedate")))
      } else {
        temp = new ListBuffer[Trade]
        temp append Trade(result.getDouble("amount"), Date.fromSQL(result.getString("tradedate")))
        companies put (result.getString("company"), new ListBuffer[Trade].append(
          Trade(result.getDouble("amount"), Date.fromSQL(result.getString("tradedate")))))
      }
    }

    val out = new ListBuffer[(String, Int, Boolean)]

    companies.foreach (company => {
      println(company._1)
      val average = companyHold(company._2)
      if (average < 0) out.append((company._1, average, false))
      else out.append((company._1, average, true))
    })

    out.toList
  }

  /**
   *  subfunction made for use in averageHoldingTime to calculate the average for each share
   *  after collecting them all together from the resultSet. This function returns an integer value
   *  which is EITHER:
   *      - the average holding period of each share in days
   *      - the total number of shares needed to balance out the shares if needed
   *
   * @param trades list of trades sent in from averageHoldingTime function
   * @return Int average or number of shares missing
   */
  private def companyHold (trades: ListBuffer[Trade]) = {
    var missingShares: Double = 0
    var totalShares: Double = 0
    val totalDateDif = new ListBuffer[Int]
    val buys = new ListBuffer[Trade]
    val sells = new ListBuffer[Trade]

    trades.foreach (trade => {
      if (trade.amount > 0) buys append trade
      else sells append trade
    })

    sells.foreach (trade => {
      var amount = -1 * trade.amount

      while (amount > 0) {

        if (buys.isEmpty) {
          missingShares += amount
          amount = 0
        } else {

          if (amount > buys.head.amount) {
            totalDateDif append (amount*(trade.date.toInt () - buys.head.date.toInt ())).toInt
            totalShares += buys.head.amount
            amount -= buys.head.amount
            buys dropInPlace 1

          } else if (amount == buys.head.amount) {
            totalDateDif append (amount*(trade.date.toInt () - buys.head.date.toInt ())).toInt
            totalShares += buys.head.amount
            amount = 0
            buys dropInPlace 1

          } else {
            buys.head.amount -= amount
            totalDateDif append (amount*(trade.date.toInt () - buys.head.date.toInt ())).toInt
            totalShares += amount
            amount = 0
          }
        }
      }
    })

    if (buys.nonEmpty) {
      val calendar = Calendar.getInstance()
      val endDate = new Date(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH)+1,
        calendar.get(Calendar.DAY_OF_MONTH))

      println(s"end date: ${endDate.toString} : ${endDate.toInt()}")

      buys.foreach(buy => {
        totalShares += buy.amount
        totalDateDif append (buy.amount.toInt * (endDate.toInt - buy.date.toInt()))
        println(buy.amount)
        println(s"buy date: ${buy.date.toString} : ${buy.date.toInt()}")
        println(s"date dif: ${totalDateDif.last/buy.amount.toInt}")
      })
    }
    println(totalDateDif)

    if (missingShares > 0) {
      -1*missingShares.toInt
    } else {
      var out = 0
      totalDateDif.foreach (dateTotal => {out += dateTotal})
      println( out)
      if (totalDateDif.nonEmpty) {
        (out / totalShares).toInt
      } else {
        0
      }
    }
  }

  def holdingStatus (accountID: String): Unit = {

    println("now checking tickers")

    val result = DB_Factory.withDB((db, statement) => db.allCompanies(statement, accountID)).get

    val companies = new ListBuffer[String]

    while (result.next()) {companies append result.getString("company")}


    println(companies)

    val tickers = companies map (name => {
      val nameTrim = name.trim
      println(name)

      val nameParts = name.split(" ").toList

      var trimName = ""



      val surl  = uri"https://query2.finance.yahoo.com/v6/finance/search?q=$nameTrim".toString()

      val con = new URL(surl).openConnection().asInstanceOf[HttpURLConnection]

      try {

//        println(url)
//        println(con.getResponseCode)
//        println(con.getResponseMessage)

        /*
        val content = Snakk.body(Snakk.url(surl, Map(
         "User-Agent"  -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0"
        )))
        */

        println(con.getContent)

        val o = JSON.parseObject(con.toString)


        val quotes = o.getJSONArray("quotes")

        quotes forEach (x => println(x))
      } catch {
        case noResponse: Exception => println(noResponse.toString)
      }
    })
  }

  def getTotalAverage (averages: List[(String, Int, Boolean)]) = {
    var  total = 0
    //flatmap?
    averages filter (a => a._2 > 0) foreach (a => total += a._2)

    total/averages.length
  }

  def getStockDetails (result: ResultSet): List[(String, Double, Double)] = {
    val list = new mutable.ListBuffer[(String, Double, Double)]
    var total = 0d

    while (result.next()) {
      total += result.getDouble("Quantity")
      list += ((result.getString("SettleDate"), result.getDouble("Quantity"), total))
    }

    list.toList
  }
}
