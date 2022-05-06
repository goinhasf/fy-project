package clients.play.auth

import clients.play.auth.config.AuthenticationClientConfig
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import endpoints4s.play.client
import shared.endpoints.authentication.AuthenticationEndpoints
import dao.users.UserInfo
import java.time.Clock
import pdi.jwt.Jwt
import pdi.jwt.JwtAlgorithm
import play.api.libs.ws.WSResponse
import endpoints4s.Tupler
import io.circe.parser._
import io.circe.syntax._
import endpoints4s.Invalid

abstract class AuthTrait(
    microServiceConfig: AuthenticationClientConfig,
    ws: WSClient,
    cc: ControllerComponents
) extends client.Endpoints(microServiceConfig.serviceUrl, ws)(
      cc.executionContext
    )
    with AuthenticationEndpoints {

  type AuthenticationToken = String

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
    headers
      .get(
        "Authorization"
      )
      .map(_.reduceRight(_ + _).stripPrefix("Bearer "))
      .map(id => (wsResponse: WSResponse) => Right(id))
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
        wsHeaders.addHttpHeaders("Authorization" -> s"Bearer ${token}")
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
