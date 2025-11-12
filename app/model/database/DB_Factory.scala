package model.database

import java.io.FileInputStream
import java.sql.{ResultSet, Statement}
import java.util.Properties

object DB_Factory {

  def createDB (url: String): DB_Interface = {
    new DB_PostgreS(url)
  }

  def withDB [T] (func: (DB_Interface, Statement) => Option[ResultSet]): Option[ResultSet] = {
    val prop = new Properties();
    prop.load(new FileInputStream("./app.properties"))
    val temp = prop.getProperty("DB_URL")
    println(temp)

    val db = DB_Factory.createDB (
      temp
      //NOTE: this will not work unless you have a file called app.properties with a working db url in it, attributed to a property called DB_URL
    )

    val statement = db.openState()

    try {
      val res = func (db, statement)
      if (res.nonEmpty) res
      else None

    } catch {
      case e:Exception => println(e)
        throw new RuntimeException(e)
    } finally {
      db.close()
    }
  }

}
