package clients.play.auth

import java.time.Clock

import scala.concurrent.Future
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import com.google.inject._
import endpoints4s.Tupler
import endpoints4s.play.client
import pdi.jwt.Jwt
import pdi.jwt.JwtClaim
import play.api.libs.ws.WSAuthScheme
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import play.api.mvc.ControllerComponents
import play.api.routing.Router
import shared.config.MicroServiceConfig
import shared.endpoints.authentication.AuthenticationEndpoints
import shared.endpoints.authorization.AuthorizationEndpoints
import shared.repositories.TokensRepository
import dao.users.UserInfo
import pdi.jwt.JwtAlgorithm

import scala.util.Failure
import scala.util.Success
import pdi.jwt.JwtHeader
import clients.play.auth.config.AuthenticationClientConfig
import play.api.Logger
import shared.endpoints.UserInfoEndpoints

@Singleton()
class AuthenticationClient @Inject() (
    microServiceConfig: AuthenticationClientConfig,
    ws: WSClient,
    cc: ControllerComponents
) extends client.Endpoints(microServiceConfig.serviceUrl, ws)(
      cc.executionContext
    )
    with AuthenticationEndpoints
    with UserInfoEndpoints
    with client.JsonEntitiesFromCodecs {

  val logger = Logger(classOf[AuthenticationClient])

  case class AuthenticationToken private[AuthenticationClient] (
      val token: String,
      val decoded: UserInfo
  )
  implicit val clock: Clock = Clock.systemUTC()

  def decodeToken(stringToken: String) = {
    val jwtConfig = microServiceConfig.jwtConfig
    Jwt
      .decode(
        stringToken,
        jwtConfig.secret,
        JwtAlgorithm
          .allHmac()
          .filter(_.name == jwtConfig.alg)
      )
      .filter(_.isValid(jwtConfig.issuer))

  }

  def authenticationToken[A](implicit
      request: Request[A]
  ): Response[AuthenticationToken] = (status, headers) => {
    val maybeToken = headers
      .get(
        "Authorization"
      )
      .map(_.reduceRight(_ + _).stripPrefix("Bearer "))

    maybeToken
      .flatMap { stringToken =>
        decodeToken(stringToken)
          .toOption
          .map(claim => (stringToken, decode[UserInfo](claim.content)))

      }
      .map { claim => (wsResponse: WSResponse) =>
        {
          val (token, parsedInfo) = claim
          parsedInfo.map(info => AuthenticationToken(token, info))
        }
      }
  }

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
    val authTokenHeader: RequestHeaders[AuthenticationToken] =
      (token, wsHeaders) => {
        wsHeaders.addHttpHeaders("Authorization" -> s"Bearer ${token.token}")
      }
    val headersWithToken = headers ++ authTokenHeader
    request(method, url, entity, requestDocs, headersWithToken)
  }

  protected def wheneverAuthenticated[A, B](
      response: Response[B]
  )(implicit request: Request[A]): Response[B] = (status, headers) => {
    if (status == Unauthorized) {
      None
    } else {
      response(status, headers)
    }
  }

}
