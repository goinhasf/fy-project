package controllers

import scala.concurrent.ExecutionContext

import endpoints4s.Tupler
import endpoints4s.Valid
import endpoints4s.play.server
import pdi.jwt.JwtClaim
import play.api.http.HeaderNames
import play.api.libs.streams.Accumulator
import play.api.mvc.BodyParser
import play.api.mvc.Results
import shared.config.JwtConfig
import shared.endpoints.authentication.Authentication
import shared.config.MicroServiceConfig
import clients.play.auth.AuthorizationClient
import play.api.mvc.DefaultControllerComponents
import play.api.http.DefaultHttpErrorHandler
import play.api.Logger
import endpoints4s.play.server.PlayComponents
import controllers.PlayComponentsController
import play.api.mvc.ControllerComponents
import utils.FileUploadConfig

abstract class ServerAuthentication(
    config: FileUploadConfig,
    errorHandler: DefaultHttpErrorHandler,
    pc: ControllerComponents,
    authClient: AuthorizationClient
) extends PlayComponentsController(pc, errorHandler)
    with Authentication
    with server.Endpoints
    with server.JsonEntitiesFromCodecs {

  implicit val ec: ExecutionContext = pc.executionContext

  case class AuthenticationToken(id: String)
  private val logger = Logger(classOf[ServerAuthentication])

  protected implicit val jwtConfig = config.jwtConfig

  def authenticationToken[A](implicit
      request: Request[A]
  ): Response[AuthenticationToken] = token => {
    Results
      .Ok(token.id)
      .withHeaders("Authorization" -> s"Bearer ${token.id}")
  }

  protected def wheneverAuthenticated[A, B](
      response: Response[B]
  )(implicit request: Request[A]): Response[B] = response

  protected def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Option[String]
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[
        H,
        AuthenticationToken,
        HCred
      ],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] = {
    val authenticationTokenRequestHeaders
        : RequestHeaders[Option[AuthenticationToken]] = { headers =>
      Valid(
        headers
          .get(HeaderNames.AUTHORIZATION)
          .map { headerValue =>
            AuthenticationToken(headerValue.stripPrefix("Bearer "))
          } match {
          case Some(token) => Some(token)
          case None        => None
        }
      )
    }
    extractMethodUrlAndHeaders(
      method,
      url,
      headers ++ authenticationTokenRequestHeaders
    )
      .toRequest[Out] {
        case (_, (headers, None)) =>
          _ =>
            Some(
              BodyParser(_ =>
                Accumulator.done(Left(Results.Unauthorized.withNewSession))
              )
            )
        case (u, (headers, Some(token))) =>
          h =>
            entity(h).map(
              _.map(e =>
                tuplerUEHCred(tuplerUE(u, e), tuplerHCred(headers, token))
              )
            )
      } { out =>
        val (ue, hCred) = tuplerUEHCred.unapply(out)
        val (u, _)      = tuplerUE.unapply(ue)
        val (h, b)      = tuplerHCred.unapply(hCred)
        (u, (h, Some(b)))
      }

  }
}
