package model.database

object DB_Accounts {

  def checkLogin (userID: Int, password: String): Boolean = {
    val conn = DB_Connect.initConn()
    val SQL = s"select * from User where UserID=$userID and password=$password"
    conn.createStatement().executeQuery(SQL).next()
  }
}
