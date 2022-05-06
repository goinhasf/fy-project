package services

import dao.users.UserInfo
import endpoints4s.play.server
import play.api.Configuration
import play.api.mvc.Results
import shared.auth.Privilege
import shared.auth.Role
import shared.endpoints.authentication.Authentication
import endpoints4s.Valid
import play.api.http.HeaderNames
import endpoints4s.Tupler
import play.api.mvc.BodyParser
import play.api.libs.streams.Accumulator
import pdi.jwt.JwtClaim

import shared.endpoints.authentication.AuthenticationEndpoints
import shared.endpoints.authorization.AuthorizationEndpoints
import shared.auth.UserCredentialsAuth
import java.time.Clock
import play.api.libs.json.Json
import shared.config.JwtConfig

trait ServerAuthorization
    extends AuthorizationEndpoints
    with server.Endpoints
    with server.JsonEntitiesFromCodecs {

  implicit val ec    = playComponents.executionContext
  implicit val clock = Clock.systemUTC()

  protected implicit def playConfiguration: Configuration

  val jwtConfig: JwtConfig

  // On server side, we build the token ourselves so we only care about the user information
  type AuthenticationToken = UserInfo

  // Encodes the user info in the JWT session
  def authenticationToken: Response[UserInfo] =
    userInfo => Results.Ok

  // Does nothing because `authenticatedReqest` already
  // takes care of returning `Unauthorized` if the request
  // is not properly authenticated
  def wheneverAuthenticated[A](response: Response[A]): Response[A] = response

}
