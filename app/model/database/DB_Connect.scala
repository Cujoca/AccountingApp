package model.database

import java.sql.{DriverManager, SQLException}

object DB_Connect {

  /**
   * Initiates a connection to the database
   * TODO: make it modular for possible distribution of this software
   *
   * @return true if successful, false if not
   */
  def initConn (): java.sql.Connection = {


    val url = "jdbc:postgresql://db-razpg-do-user-199358-0.i.db.ondigitalocean.com:25060/test?user=andreitest&password=AVNS_PBjQepzKsExgIjZet_k&sslmode=require"
    val conn = try {
      var conn = DriverManager.getConnection (url)
      conn
    } catch {
      case e: SQLException => throw new RuntimeException(e)
    }
    conn
  }
}
