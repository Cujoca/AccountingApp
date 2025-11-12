package controllers

import model.database.{DB_Access, DB_Accounts, DB_Factory}
import model.domain.{Account, Date, Report, Transaction, TransactionsDBHandler, Users}
import play.api.data.Form
import play.api.data.Forms.{mapping, number, text}
import play.api.i18n.I18nSupport
import play.api.i18n.Messages._
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext

/**
 * Form for keeping track of which account to get info about
 * @param name: String, the ID of the requested Account
 */
case class EditAccountData(name: String)
object EditAccountData {
  def unapply(nameData: EditAccountData): Option[String] = {
    Some(nameData.name)
  }
}

/**
 * Form for adding new transaction into a given account
 * contains data about the transaction
 *
 * @param name    : String, name of company which
 * @param amount  : String, amount of shares which the trade contains
 *                          (later changed to double, couldn't figure out
 *                           how to put double into a form like this)
 * @param date    : String, string representation of the date when this trade takes place
 */
case class AddTxnData (name: String, amount: String, date: String)
object AddTxnData {
  def unapply(txnData: AddTxnData): Option[(String, String, String)] = {
    Some(txnData.name, txnData.amount, txnData.date)
  }
}

/**
 * Controller class to take care of all processes and pages related to users and accounts informations.
 * Including getting lists of accounts, viewing and changing account names, and viewing reports
 * of different accounts' trading history and habits.
 *
 * @param cc: MessagesControllerComponents, ignore this, implicitly used
 */
@Singleton
class Users_Controller @Inject()(val cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

  import play.api.data.validation.Constraints._

  val editAccountForm: Form[EditAccountData] = Form(
    mapping(
      "name" -> text
    )(EditAccountData.apply)(EditAccountData.unapply)
  )

  val addTxnForm: Form[AddTxnData] = Form(
    mapping(
      "name"    -> text,
      "amount"  -> text,
      "date"    -> text
    )(AddTxnData.apply)(AddTxnData.unapply)
  )

  /*def login() = Action {implicit request =>
    Ok(views.html.login(form))
  }*/

  /**
   * go to accounts list
   */
  def viewAccounts() = Action {implicit request: Request[AnyContent] =>
    val handler = new TransactionsDBHandler
    Ok(views.html.accountNums(handler.getAccountsByUser))
  }

  /**
   * go to edit account page
   * @param accountID: String, account to be edited
   */
  def editAccount(userID: String, accountID: String)= Action {implicit request  =>
    val handler = new TransactionsDBHandler
    val htt = handler.getAccount(accountID).get

    editAccountForm.bind(Map(
      "name" -> htt.name
    ))
    Ok(views.html.editAccount(htt, editAccountForm))
  }

  /**
   * form binding for remembering which account to be looking at. eventually redirects to
   * the edit account page
   * @param accountID: String, account to be editing
   */
  def userPost(userID: String, accountID:String) = Action { implicit request =>
    println("userPost")
    Account.getByID(accountID).map {account=>
      editAccountForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            // binding failure, you retrieve the form containing errors:
            println("in bad request")
            BadRequest(views.html.editAccount(Account.getByID(accountID).get, formWithErrors))
          },
          userData => {
            /* binding success, you get the actual value. */
            val newUser = Account(accountID, userData.name, account.userID)
            println("in good request" + newUser)
            Account.update(newUser)
            Redirect(routes.Users_Controller.editAccount(userID,  accountID))
          }
        )
    }.getOrElse{
      NotFound ("AccountID not found:" + accountID)
    }
  }

  /**
   * Adding new transaction manually to balance out transactions for reports
   *
   * @param accountID:  Account that the new transaction will be added to
   * @return:           Action{implicit request}, will redirect upon completion
   */
  def txnPost (accountID: String) = Action { implicit request =>
    println("txnPost")
      addTxnForm
        .bindFromRequest()
        .fold(
          formWithErrors => {
            // binding failure, you retrieve the form containing errors:
            println("in bad request")
            BadRequest(views.html.addedTxn("was unable to post txn", accountID))
          },
          userData => {
            /* binding success, you get the actual value. */
            val addTxn = new Transaction("ADDED", Date.fromString(userData.date),
              Date.fromString(userData.date), userData.name, "BUY",
              userData.amount.toDouble, 0, 0, 0, "", accountID)
            println("in good request" + addTxn)
            val handler = new TransactionsDBHandler
            handler.addTxn(addTxn)
            Redirect(routes.Users_Controller.addedTxn("transaction was successfully added", accountID))
          }
        )
  }

  /**
   * redirects to the screen which gives info about completed transaction addition
   *
   * @param message   : String, message to be given to user on status of addition
   * @param accountID : String, account to which the transaction was added
   * @return          : Action{implicit request}, redirection to the added txn screen
   */
  def addedTxn (message: String, accountID: String) = Action { implicit request =>

    Ok(views.html.addedTxn(message, accountID))
  }

  /**
   * redirect to accountReport page
   * @param accountID: String, account to create and view report of
   */
  def getAccountReport (userID: String, accountID: String) = Action {implicit request =>
    Redirect(routes.Users_Controller.viewReport(userID, accountID))
  }

  /**
   * create report and then go to html page to view it
   * @param accountID: String, account to make report for
   */
  def viewReport (userID: String, accountID: String) = Action {implicit request =>

    DB_Factory.withDB((db, statement) => db.getDivTotals(statement, accountID))

    val report = Report.addDivToReport(accountID)
    val average = Report.averageReport(accountID)
    val averageTotal = Report.getTotalAverage(average)

    //Report.holdingStatus(accountID)

    Ok(views.html.accountReport(accountID, report, average, averageTotal, addTxnForm))
  }

  /**
   * gets all users from database and displays in a table
   *
   * @return  : Action{implicit request}, redirects to the users screen
   */
  def viewUsers () = Action {implicit request =>
    val users = Users.grabUsers
    Ok(views.html.users(users))
  }

  /**
   * gets of a user's accounts, and provides a simplified report for each
   * along with a button to view more details about specific accounts
   *
   * @param userID  : String, the user to find info about
   * @return        : Action, redirects to the user details page
   */
  def viewUserDetail (userID: String) = Action {implicit request =>

    val accounts = Users.getAccountsByUser(userID)
    val reports = Report.parseReportList(accounts)

    Ok(views.html.userDetails(userID, reports))
  }

  def viewStockDetail (userID: String, account: String, name: String) = Action {implicit request =>

    val handler = new TransactionsDBHandler

    val result = handler.getStockDetail(account, name)

    Ok(views.html.stockDetails(result, name))


  }


}
