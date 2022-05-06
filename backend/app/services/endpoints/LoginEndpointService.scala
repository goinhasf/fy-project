package services.endpoints

import javax.security.auth.login.CredentialException

import scala.concurrent.Future

import akka.compat.Future
import clients.play.auth.AuthorizationClient
import com.google.inject._
import dao.users.UserInfo
import endpoints4s.Tupler
import endpoints4s.Valid
import endpoints4s.play.server
import pdi.jwt.Jwt
import pdi.jwt.JwtClaim
import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HeaderNames
import play.api.libs.streams.Accumulator
import play.api.mvc.BodyParser
import play.api.mvc.DefaultControllerComponents
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.routing.Router
import play.api.routing.SimpleRouter
import services.EndpointService
import shared.config.JwtConfig
import shared.config.MicroServiceConfig
import shared.endpoints.authentication.AuthenticationEndpoints

import scala.util.Failure
import scala.util.Success
import endpoints4s.play.server.PlayComponents

@Singleton()
class LoginEndpointService @Inject() (
    config: MicroServiceConfig,
    authorizationClient: AuthorizationClient,
    pc: PlayComponents
) extends ServerAuthentication(
      config,
      authorizationClient,
      pc
    )
    with AuthenticationEndpoints {

  def routes: Router.Routes = routesFromEndpoints(loginEndpoint)

  val loginEndpoint = login.implementedByAsync { service =>
    val (credentials, _) = service
    authorizationClient
      .createToken(credentials)
      .map(tokenId => Some(AuthenticationToken(tokenId)))
  }

}
