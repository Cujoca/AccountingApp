package model.domain

import java.io.{BufferedReader, FileReader}

object csvReaderTester extends App{

  val trades = new collection.mutable.HashSet[Transaction]

  val reader = new BufferedReader(new FileReader("./src/Resources/TransactionExample.csv"))

  reader.readLine()
  reader.readLine()
  reader.readLine()
  reader.readLine()

  while (reader.ready) {
    val inStr = reader.readLine
    val tran = Transaction.TransFromCSV(inStr.split(","))
    trades add tran
  }
}
