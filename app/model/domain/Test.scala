package model.domain

import model.database.DB_Factory

import java.io.{BufferedReader, File, FileReader, FileWriter}
import scala.collection.mutable

object Test extends App{

  /*
  val result = DB_Factory.withDB((db, state) => db.getTickers(state)).get

  val file = new File("./app/model/Resources/tickers.csv")

  println(file.getAbsolutePath)

  val fileWriter = new FileWriter(file)

  while (result.next) {
    val temp = result.getString("name")
    fileWriter.write(temp+",\n")
  }
  fileWriter.close()
  */

  val file = new File("./app/model/Resources/tickers.csv")

  val fileReader = new BufferedReader(new FileReader(file))

  while (fileReader.ready()) {
    val parts = fileReader.readLine().split(",")

    val regex = "_*\\.V".r

    if (regex.findAllIn(parts(1)).isEmpty) {
      DB_Factory.withDB((db, statement) =>
        db.setTicker(statement, parts(0), parts(1), "Tor"))
    } else {
      DB_Factory.withDB((db, statement) =>
        db.setTicker(statement, parts(0), parts(1), "Van"))
    }
  }
}
