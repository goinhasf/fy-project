package clients.play.auth

import com.google.inject._
import shared.config.MicroServiceConfig
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import endpoints4s.play.client
import shared.endpoints.authorization.AuthorizationEndpoints
import endpoints4s.Tupler
import play.api.routing.Router
import play.api.libs.ws.WSResponse
import play.api.libs.ws.WSAuthScheme
import scala.concurrent.Future
import play.api.libs.ws.WSRequest
import shared.repositories.TokensRepository
import pdi.jwt.JwtClaim
import pdi.jwt.Jwt
import java.time.Clock

@Singleton()
class AuthorizationClient @Inject() (
    microServiceConfig: MicroServiceConfig,
    ws: WSClient,
    cc: ControllerComponents
) extends client.Endpoints(microServiceConfig.serviceUrl, ws)(
      cc.executionContext
    )
    with AuthorizationEndpoints
    with client.JsonEntitiesFromCodecs
