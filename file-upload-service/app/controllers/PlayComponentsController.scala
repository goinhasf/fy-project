package controllers

package controllers
import com.google.inject._
import play.api.mvc.ControllerComponents
import endpoints4s.play.server.PlayComponents
import play.api.mvc.DefaultActionBuilder
import scala.concurrent.ExecutionContext
import play.api.mvc.PlayBodyParsers
import play.api.http.FileMimeTypes
import play.api.http.HttpErrorHandler
import play.api.http.DefaultHttpErrorHandler

@Singleton
class PlayComponentsController @Inject() (
    val cc: ControllerComponents,
    errorHandler: DefaultHttpErrorHandler
) {
  val playComponents = new PlayComponents {

    override def playBodyParsers: PlayBodyParsers   = cc.parsers
    override def httpErrorHandler: HttpErrorHandler = errorHandler
    override def defaultActionBuilder: DefaultActionBuilder =
      DefaultActionBuilder(cc.parsers.anyContent)(cc.executionContext)
    override def fileMimeTypes: FileMimeTypes = cc.fileMimeTypes
    override implicit def executionContext: ExecutionContext =
      cc.executionContext

  }

  val Action = cc.actionBuilder
}
