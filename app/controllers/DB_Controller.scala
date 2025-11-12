package controllers

import model.database.DB_Factory

import javax.inject._
import play.api._
import play.api.mvc._
import model.domain.TransactionsDBHandler

@Singleton
class DB_Controller @Inject()(val controllerComponents: ControllerComponents) extends BaseController{

  def txns() = Action {implicit request: Request[AnyContent] =>
    val handler = new TransactionsDBHandler
    handler.getFromSQL()
    Ok(views.html.txns(handler.toList))
  }

  def deleteDB() = Action {implicit request: Request[AnyContent] =>
    DB_Factory.withDB((db, statement) => db.dropDB(statement))
    Ok(views.html.deletedBase())
  }
}
