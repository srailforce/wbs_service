package controllers

import javax.inject._

import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc._

import scala.util.{Try, Success, Failure}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(private val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
  extends AbstractController(cc) {

  lazy val db = dbConfigProvider.get.db

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def loadDb(): Action[AnyContent] = Action async { implicit request: Request[AnyContent] =>
    import scala.concurrent.ExecutionContext.Implicits.global
    import slick.jdbc.PostgresProfile.api._
    val currentDate = sql"select CURRENT_TIMESTAMP ".as[java.sql.Timestamp]
    db.run(currentDate).map(v => v(0)).transform {
      case Success(s) => Success(Ok(s.toString))
      case Failure(_) => Success(NoContent)
    }
  }
}
