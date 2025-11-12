package controllers

import model.database.{DB_Factory, DB_StartUp}
import model.domain.{FileHandler, Report, TransactionsDBHandler}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import javax.inject._
import org.apache.pekko.stream.IOResult
import org.apache.pekko.stream.scaladsl._
import org.apache.pekko.util.ByteString
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.streams._
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.core.parsers.Multipart.FileInfo

import java.util
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

case class FormData(name: String, accountNum: String, userID: String)
object FormData {
  def unapply(formData: FormData): Option[(String, String, String)] = {
    Some(formData.name, formData.accountNum, formData.userID)
  }
}

case class TickerData (ticker: String, market: String)
object TickerData {
  def unzipWithApply(formData: TickerData): Option[(String, String)] = {
    Some(formData.ticker, formData.market)
  }
}

/**
 * This controller handles a file upload.
 */
@Singleton
class FileUpload_Controller @Inject() (cc:MessagesControllerComponents)
                               (implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = Logger(this.getClass)

  var addAccount = false

  val form: Form[FormData] = Form(
    mapping(
      "name" -> text,
      "accountNum" -> text,
      "userID" -> text
    )(FormData.apply)(FormData.unapply)
  )

  val tickerForm: Form[TickerData] = Form(
    mapping(
      "ticker" -> text,
      "market" -> text
    )(TickerData.apply)(TickerData.unapply)
  )

  /**
   * Renders a start page.
   */
  def showFormUpdateFile: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.files(form, List.empty))
  }

  type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

  /**
   * Uses a custom FilePartHandler to return a type of "File" rather than
   * using Play's TemporaryFile class.  Deletion must happen explicitly on
   * completion, rather than TemporaryFile (which uses finalization to
   * delete temporary files).
   *
   * @return
   */
  private def handleFilePartAsFile: FilePartHandler[File] = {
    case FileInfo(partName, filename, contentType, _) =>
      val path: Path = Files.createTempFile("multipartBody", "tempFile")
      val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(path)
      val accumulator: Accumulator[ByteString, IOResult] = Accumulator(fileSink)
      accumulator.map {
        case IOResult(count, status) =>
          logger.info(s"count = $count, status = $status")
          FilePart(partName, filename, contentType, path.toFile)
      }
  }

  /**
   * A generic operation on the temporary file that deletes the temp file after completion.
   */
  private def operateOnTempFile(file: File, account: String, userID: String) = {
    val size = Files.size(file.toPath)
    logger.info(s"size = ${size}")
    val reportData = FileHandler.readFile(file.toPath.toString, account, userID)
    println(reportData)
    Files.deleteIfExists(file.toPath)
    reportData
  }

  /**
   * Uploads a multipart file as a POST request.
   *
   * @return
   */
  def upload: Action[MultipartFormData[File]] = Action(parse.multipartFormData(handleFilePartAsFile)) { implicit request =>
    // Derive account prefix from each filename (prefix before first '-')
    def accountFromFilename(name: String): String = {
      val parts = name.split("-", 2)
      if (parts.nonEmpty) parts(0) else ""
    }

    val derivedAccounts = new mutable.HashSet[String]()
    request.body.files.foreach { fp =>
      val prefix = accountFromFilename(fp.filename)
      if (prefix.nonEmpty) derivedAccounts.add(prefix)
    }

    // Extract posted account and userId, if present
    val postedAccountOpt = request.body.dataParts.get("accountNum").flatMap(_.headOption)
    val userIdOpt = request.body.dataParts.get("userID").flatMap(_.headOption)

    // Validate and possibly short-circuit with a redirect containing an error flash
    val derivedAccountOpt = derivedAccounts.headOption
    val earlyError: Option[Result] = {
      if (derivedAccounts.size > 1) {
        val msg = "Can not upload files from different accounts. Please select files with the same account number prefix."
        logger.warn(msg + s" Derived: ${derivedAccounts.mkString(",")}")
        Some(Redirect(routes.FileUpload_Controller.showFormUpdateFile()).flashing("error" -> msg))
      } else {
        (for {
          posted <- postedAccountOpt
          derived <- derivedAccountOpt
          if posted != derived
        } yield {
          val msg = s"Entered account number ($posted) does not match files' account number ($derived)."
          logger.warn(msg)
          Redirect(routes.FileUpload_Controller.showFormUpdateFile()).flashing("error" -> msg)
        })
      }
    }
    earlyError.getOrElse {
      // Decide the account we will use: posted if present, otherwise derived (may be empty for 0 files)
      val effectiveAccount: String = postedAccountOpt.orElse(derivedAccountOpt).getOrElse("")
      val effectiveUserId: String = userIdOpt.getOrElse("")

      val fileOption = request.body.files.toArray.map {
        case FilePart(key, filename, contentType, file, fileSize, dispositionType, _) =>
          logger.info(s"key = $key, filename = $filename, contentType = $contentType, file = $file, fileSize = $fileSize, dispositionType = $dispositionType")
          println(filename)
          (filename, operateOnTempFile(file, effectiveAccount, effectiveUserId).toMap)
      }
      val uploaded = fileOption.isEmpty

      var totalBuy: Int = 0
      var totalSell: Int = 0
      var totalOther: Int = 0
      var totalNet: BigDecimal = 0
      val tickers = new mutable.HashMap[String, (String, String)]

      fileOption.foreach (file => {
        file._2.foreach(company => {
          totalBuy+= company._2.buyOrder
          totalSell+= company._2.sellOrder
          totalOther+= company._2.otherOrder
          totalNet = totalNet + company._2.profit
          val result = DB_Factory.withDB((db, statement) => db.getSpecificTick(statement, company._1)).get
          result.next
          tickers put (company._1, (result.getString("ticker"), result.getString("market")))
        })
      })

      Ok(views.html.fileStatus(uploaded, fileOption, totalBuy,
        totalSell,totalOther, totalNet.toDouble, tickers))
    }
  }

  def editStockTicker (stockName: String) = Action {implicit request =>
    val result = DB_Factory.withDB((db, state) => db.getSpecificTick(state, stockName)).get
    result.next
    val stockInfo = (result.getString("name"), result.getString("ticker"), result.getString("market"))

    Ok(views.html.tickerChange(stockName, stockInfo, tickerForm))
  }

  def tickerPost (name: String) = Action { implicit request =>
    println("tickerpost")
    tickerForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          println("in bad request")
          BadRequest(views.html.tickerStatus("bad request"))
        },
        tickerData => {
          println("in good request")
          val handler = new TransactionsDBHandler
          handler.addTicker(name, tickerData.ticker, tickerData.market)
          Redirect(routes.FileUpload_Controller.editStockTicker(name))
        }
    )
  }

  def tickerStatus (arg: String) = Action { implicit request =>
    Ok(views.html.tickerStatus(arg))
  }

  def tickers () = Action { implicit request =>
    val handler = new TransactionsDBHandler
    handler.getTickers()
    println(handler.tickerData)
    Ok(views.html.tickers(handler.tickerData))
  }
}