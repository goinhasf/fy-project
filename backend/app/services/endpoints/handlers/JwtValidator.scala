package services.endpoints.handlers

import clients.play.auth.AuthorizationClient
import scala.concurrent.Future
import play.api.mvc.Results
import play.api.http.Status
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext
import play.api.mvc.Result
import dao.users.UserInfo
import shared.config.JwtConfig
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import scala.util.Failure
import scala.util.Success

trait JwtValidator extends ActionHandlers {

  val authClient: AuthorizationClient
  type JwtValidator = AsyncRequestHandler[String]
  type JwtHandler   = AsyncRequestHandler[UserInfo]
  def jwtValidatorFromHeader(implicit
      ec: ExecutionContext
  ): JwtValidator =
    request => {
      request
        .headers
        .get("Authorization")
        .map(_.stripPrefix("Bearer "))
        .map(
          authClient
            .authorize(_)
            .map(_ match {
              case Some(value) => Right(value)
              case None        => Left(Results.Unauthorized("Jwt is invalid"))
            })
        ) match {
        case Some(value) => value
        case None =>
          Future.successful(
            Left(Results.Unauthorized("Missing Authorization header"))
          )
      }
    }

  def sessionJwtValidator(
      onFailed: => Result
  )(implicit ec: ExecutionContext): JwtValidator =
    request => {
      request
        .session
        .data
        .get("jwtId")
        .map(
          authClient
            .authorize(_)
            .flatMap(_ match {
              case Some(value) => authClient.getTokenContent(value)
              case None        => Future.successful(None)
            })
            .map(_ match {
              case Some(content) => {
                request.session.data + ("userInfo" -> content)
                Right(content)
              }
              case None => Left(onFailed)
            })
        ) match {
        case Some(value) => value
        case None =>
          Future.successful(
            Left(onFailed)
          )
      }
    }

  def sessionOrAuthorizationValidator(
      onFailed: => Result
  )(implicit ec: ExecutionContext, jwtConfig: JwtConfig): JwtHandler = req => {
    def handleJwt(jwtId: String) = {

      authClient
        .getTokenContent(jwtId)
        .map(_ match {
          case Some(content) =>
            decode[UserInfo](content) match {
              case Left(value) =>
                Left(Results.BadRequest("Could not decode jwt content"))
              case Right(value) => Right(value)
            }
          case None => Left(Results.BadRequest("Could not find jwt"))
        })
    }

    jwtValidatorFromHeader
      .apply(req)
      .flatMap(_ match {
        case Left(value) =>
          sessionJwtValidator(onFailed)
            .apply(req)
            .flatMap(_ match {
              case Left(value) => Future.successful(Left(value))
              case Right(content) =>
                decode[UserInfo](content) match {
                  case Left(value) =>
                    Future.successful(
                      Left(Results.BadRequest("Could not decode jwt content"))
                    )
                  case Right(value) => Future.successful(Right(value))
                }
            })
        case Right(value) => handleJwt(value)
      })
  }

  def defaultJwtValidator(implicit ec: ExecutionContext, jwtConfig: JwtConfig) =
    sessionOrAuthorizationValidator(
      Results.Unauthorized("Jwt missing")
    )
}
