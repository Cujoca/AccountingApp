package model.domain

/**
 * Date class to record time of transaction
 *
 * @param year  int year of transaction
 * @param month int month of transaction
 * @param day   int day of transaction
 */
class Date (year: Int = -1, month: Int = -1, day: Int = -1){

  /**
   * gets date as readable string in a DD/MM/YYYY format
   *
   * @return stringified date
   */
  override def toString: String = {
    s"""$day/$month/$year"""
  }

  /**
   * gets date as YYYY/MM for report making
   *
   * @return report-ified date
   */
  def toStringMMYY: String = {
    s"""$year/$month"""
  }

  /**
   * gives the date object in a sql allowed form
   *
   * @return sql-ified string
   */
  def toSQL: String = {
    s"""$year-$month-$day"""
  }

  /**
   * returns this date instance as an integer value of days
   *
   * @return Int - this date as a total number of days
   */
  def toInt (): Int = {
    (this.year*365)+(Date.monthDays(this.month))+this.day
  }

  def < (comp: Date): Boolean = {
    if (this.year <= comp.getYear)
      if (this.month <= comp.getMonth)
        if (this.day < comp.getDay) return true
    false
  }
  def getYear: Int= this.year
  def getMonth: Int = this.month
  def getDay: Int = this.day
}


/**
 * companion object for date which has some functions
 * to parse from csv files or from the sql results
 */
object Date {

  /**
   * Trades string month for int representation
   *
   * @param month string from csv file
   * @return int equivalent
   */
  private def getMonth(month: String): Int = {
    month match {
      case "Jan" => 1
      case "Feb" => 2
      case "Mar" => 3
      case "Apr" => 4
      case "May" => 5
      case "Jun" => 6
      case "Jul" => 7
      case "Aug" => 8
      case "Sep" => 9
      case "Oct" => 10
      case "Nov" => 11
      case "Dec" => 12
      case _ => 13
    }

  }

  private def monthDays (month: Int) = {
    month match {
      case 1 =>   0
      case 2 =>   31
      case 3 =>   59
      case 4 =>   90
      case 5 =>   120
      case 6 =>   151
      case 7 =>   181
      case 8 =>   212
      case 9 =>   243
      case 10 =>  273
      case 11 =>  304
      case 12 =>  334
      case _ => -1
    }
  }

  /**
   * for creating a date after grabbing from csv file
   *
   * @param arg the date as represented in the csv file <br>
   *            "DD Mmm YYYY"
   * @return new date instance
   */
  def fromString(arg: String): Date = {
    val parts = arg split " "
    new Date(parts(2).toInt, getMonth(parts(1)), parts(0).toInt)
  }

  /**
   * for creating a date after grabbing from sql query
   *
   * @param arg String value from sql query
   * @return new date instance
   */
  def fromSQL (arg: String): Date = {
    val parts = arg split "-"
    new Date(parts(0).toInt, parts(1).toInt, parts(2).toInt)
  }
}
