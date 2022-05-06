package services.endpoints.handlers

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import play.api.mvc.Results
import play.api.mvc.Session
import play.api.Logger

trait SessionHandler extends ActionHandlers {
  type SessionInfo             = Session
  type SessionContentExtractor = RequestHandler[SessionInfo]
  val sessionHandlerLogger = Logger(classOf[SessionHandler])

  def extractOptValueFromSession(key: String): RequestHandler[Option[String]] =
    header => {
      sessionHandlerLogger.debug(header.session.data.get(key).toString())
      Right(
        header
          .session
          .data
          .get(key)
      )
    }

  def extractValueFromSession(
      key: String
  ) = extractValueFromSession[String](key, _.asJson.noSpaces)

  def extractValueFromSession[T](
      key: String,
      transform: String => String = x => x
  )(implicit decoder: Decoder[T]): RequestHandler[T] = header => {
    sessionHandlerLogger.debug(header.session.data.get(key).toString())
    header
      .session
      .data
      .get(key)
      .toRight(Results.BadRequest(s"Session key '$key' not found"))
      .flatMap(value =>
        decode[T](transform(value))
          .left
          .map { error =>
            sessionHandlerLogger.debug(error.getMessage())
            Results.BadRequest(
              s"Could not decode session info with key '$key' because ${error.getMessage()}"
            )
          }
      )
  }

  def sessionExtractor: SessionContentExtractor =
    header =>
      Right(
        header.session
      )

  def saveToSession[A, B]: AsyncResponseHandler[A, B] = ???
}
