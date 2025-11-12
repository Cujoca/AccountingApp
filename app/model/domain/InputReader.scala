package model.domain

import java.util.Scanner
import scala.util.Try

/**
 * Util library to get input from user
 * TEST FUNCTIONS, ONLY FOR DEBUGGING
 */
trait InputReader {

  /**
   * Function to get a valid integer input from user
   * 
   * @param sc scanner to read input
   * @return if invalid input then recursively call
   *         else return valid input
   */
  def getInt(sc: Scanner): Int = {
    println("Please enter your choice: ")
    // get user input and turn into option when converting
    val tempInt = Try(sc.nextLine().toInt).toOption
    // if option is none, then recursively call
    if (tempInt == None)
      println ("Invalid input, try again")
      return getInt(sc)
    // else return contents of option  
    tempInt.get
    }

  /**
   * Function to get a valid float input from user
   * 
   * @param sc scanner to read input
   * @return if invalid input the recursively call
   *         else return valid input
   */
  def getFloat(sc: Scanner): Float = {
    println("Please enter your choice: ")
    // get user input and turn into option when converting
    val tempFloat = Try(sc.nextLine().toFloat).toOption
    // if option is none, then recursively call
    if (tempFloat.isEmpty)
      println ("Invalid input, try again")
      return getInt(sc)
    // else return contents of option  
    tempFloat.get
  }
  
  
}
