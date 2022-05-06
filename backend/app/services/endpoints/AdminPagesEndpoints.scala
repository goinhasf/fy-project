package services.endpoints

import com.google.inject._
import endpoints4s.play.server.PlayComponents
import shared.config.MicroServiceConfig
import clients.play.auth.AuthorizationClient
import endpoints4s.play.server
import services.endpoints.handlers.ActionHandlers
import services.endpoints.handlers.JwtValidator
import services.endpoints.handlers.SessionHandler
import play.api.routing.Router
import play.api.mvc.RequestHeader
import play.filters.csrf.CSRF
import scala.concurrent.Future
import views.SinglePageAppView
import services.endpoints.handlers.CsrfHandler
import views.AppTemplate
import dao.users.UserInfo
import shared.endpoints.pages.LoginEndpoint
import shared.auth.AdminRole
import shared.auth.GuildAdminRole
import play.api.Logger

class AdminPagesEndpoints @Inject() (
    pc: PlayComponents,
    microServiceConfig: MicroServiceConfig,
    override val authClient: AuthorizationClient
) extends ServerAuthentication(microServiceConfig, authClient, pc)
    with shared.endpoints.pages.AdminPagesEndpoints
    with server.JsonEntitiesFromCodecs
    with LoginEndpoint
    with HtmlEndpoints
    with ActionHandlers
    with JwtValidator
    with CsrfHandler
    with SessionHandler {

  val logger = Logger(classOf[AdminPagesEndpoints])

  def routes: Router.Routes = routesFromEndpoints(
    EndpointWithHandler1[Unit, AppTemplate, CSRF.Token](
      adminLoginPage,
      csrfExtractor,
      (_, token) =>
        Future.successful(
          SinglePageAppView
            .getSinglePageAppView("Society Management", token)
        )
    ),
    protectedRouteHandler(adminMainPage),
    protectedRouteHandler(adminEventSubmissionsPage)
  )

  def redirectAction = redirect(adminLoginPage)(()).apply(())

  def isAdminActionHandler: AsyncRequestHandler[UserInfo] = req =>
    sessionOrAuthorizationValidator(redirectAction)
      .apply(req)
      .map(_.flatMap { userInfo =>
        val roleType = userInfo.role.roleType

        if (roleType == AdminRole() || roleType == GuildAdminRole()) {
          logger.info(s"An Admin has logged in. ${userInfo}")
          Right(userInfo)
        } else {
          logger.info(s"Unauthorised user tried to access page. ${userInfo}")
          Left(redirect(unauthorizedPage)(()).apply(()))
        }
      })

  def protectedRouteHandler[A](
      endpoint: Endpoint[A, Option[HtmlPage]]
  ): EndpointWithHandler1[A, Option[HtmlPage], CSRF.Token] =
    EndpointWithHandler1(
      endpoint,
      actionHandlerUnion(
        csrfExtractor,
        isAdminActionHandler
      )((token, _) => token),
      (_, token) => {
        Future.successful(
          Some(
            SinglePageAppView
              .getSinglePageAppView("Society Management", token)
          )
        )
      }
    )

  def createPlayHandler[A](
      endpoint: Endpoint[A, Option[HtmlPage]]
  ): EndpointWithHandler1[A, Option[HtmlPage], CSRF.Token] =
    EndpointWithHandler1(
      endpoint,
      actionHandlerUnion(
        csrfExtractor,
        sessionJwtValidator(redirectAction)
      )((token, _) => token),
      (_, token) => {
        Future.successful(
          Some(
            SinglePageAppView
              .getSinglePageAppView("Society Management", token)
          )
        )
      }
    )

}
