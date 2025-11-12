package model.database

import org.postgresql.util.PSQLException

object DB_Clear {

  def clearDB (): Unit = {
    try {
      DB_Connect.initConn ().prepareStatement ("drop table if exists Transaction ").executeQuery ()
    } catch {
      case e:PSQLException => println(e)
    }
    try {
      DB_Connect.initConn ().prepareStatement ("drop table if exists Account ").executeQuery ()
    } catch {
      case e:PSQLException => println(e)
    }
    try {
      DB_Connect.initConn ().prepareStatement ("drop table if exists SiteUser ").executeQuery ()
    } catch {
      case e:PSQLException => println(e)
    }
    println("Database has been cleared")
  }

}
