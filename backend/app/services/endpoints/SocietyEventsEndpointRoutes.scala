package services.endpoints

import com.google.inject._
import shared.config.MicroServiceConfig
import clients.play.auth.AuthorizationClient
import endpoints4s.play.server.PlayComponents
import play.api.routing.Router
import shared.endpoints.events.wizard.SocietyEventsEndpoints
import services.endpoints.handlers.JwtValidator
import services.impl.SocietyEventsService
import services.endpoints.handlers.SessionHandler
import dao.users.UserInfo

@Singleton
class SocietyEventsEndpointRoutes @Inject() (
    config: MicroServiceConfig,
    override val authClient: AuthorizationClient,
    societyEventsService: SocietyEventsService,
    playComponents: PlayComponents
) extends ServerAuthentication(
      config,
      authClient,
      playComponents
    )
    with SocietyEventsEndpoints
    with JwtValidator
    with SessionHandler {

  def routes: Router.Routes = routesFromEndpoints(
    EndpointWithHandler2(
      getSocietyEvents,
      defaultJwtValidator,
      extractOptValueFromSession("societyId"),
      (
          q: SocietyEventsEndpoints.SocietyEventStatusQ,
          userInfo: UserInfo,
          societyId: Option[String]
      ) => societyEventsService.getSocietyEvents(userInfo, societyId, q)
    ),
    EndpointWithHandler2(
      getSocietyEventDetails,
      defaultJwtValidator,
      extractOptValueFromSession("societyId"),
      societyEventsService.getSocietyEvent
    ),
    EndpointWithHandler2(
      reviewEvent,
      defaultJwtValidator,
      extractOptValueFromSession("societyId"),
      societyEventsService.reviewEvent
    )
  )
}
