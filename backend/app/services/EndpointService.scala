package services

import javax.inject.Inject

import scala.concurrent.ExecutionContext

import com.google.inject._
import endpoints4s.play.server.PlayComponents
import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.http.FileMimeTypes
import play.api.http.HttpErrorHandler
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.DefaultControllerComponents
import play.api.mvc.PlayBodyParsers
import play.api.mvc.Session
import play.api.routing.SimpleRouter
import play.filters.csrf.CSRF
import services.endpoints.handlers.ActionHandlers
import endpoints4s.play.server

@Singleton()
class PlayComponentsProvider @Inject() (
    controllerComponents: DefaultControllerComponents,
    defaultErrorHandler: DefaultHttpErrorHandler
) extends PlayComponents {
  def playBodyParsers: PlayBodyParsers   = controllerComponents.parsers
  def httpErrorHandler: HttpErrorHandler = defaultErrorHandler
  def defaultActionBuilder: DefaultActionBuilder =
    controllerComponents.actionBuilder
  def fileMimeTypes: FileMimeTypes = controllerComponents.fileMimeTypes
  implicit def executionContext: ExecutionContext =
    controllerComponents.executionContext
}

abstract class EndpointService(
    override val playComponents: PlayComponents
) extends SimpleRouter
    with ActionHandlers
    with server.Endpoints {
  type CsrfActionHandler    = RequestHandler[Option[CSRF.Token]]
  type SessionActionHandler = RequestHandler[Session]
  implicit val ec: ExecutionContext  = playComponents.executionContext
}
