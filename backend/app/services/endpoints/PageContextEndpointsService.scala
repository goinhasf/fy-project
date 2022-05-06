package services.endpoints

import scala.concurrent.Future

import clients.play.auth.AuthorizationClient
import com.google.inject._
import dao.societies.Society
import dao.users.UserInfo
import data.repositories.SocietiesRepository
import endpoints4s.Tupler
import endpoints4s.Validated
import endpoints4s.algebra.MuxRequest
import endpoints4s.play.server
import endpoints4s.play.server.MuxHandlerAsync
import endpoints4s.play.server.PlayComponents
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import io.circe.parser._
import io.circe.syntax._
import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.SimpleRouter
import services.contexts.AllContextServices
import services.endpoints.handlers._
import shared.auth.Privilege
import shared.config.JwtConfig
import shared.config.MicroServiceConfig
import shared.endpoints.context.PageContextEndpoints
import shared.endpoints.pages.LoginEndpoint
import shared.pages._
import shared.pages.content._

@Singleton()
class PageContextEndpointsService @Inject() (
    config: MicroServiceConfig,
    override val authClient: AuthorizationClient,
    playComponents: PlayComponents,
    allContextServices: AllContextServices,
    societiesRepository: SocietiesRepository
) extends ServerAuthentication(
      config,
      authClient,
      playComponents
    )
    with PageContextEndpoints
    with MuxEndpointsWithHeaders
    with HtmlEndpoints
    with LoginEndpoint {

  val logger = Logger(classOf[PageContextEndpointsService])

  override def routes: Router.Routes = routesFromEndpoints(
    getAppContextEndpoint
  )

  type RedirectQuery = Unit
  def redirectEndpoint: Endpoint[RedirectQuery, _]          = loginPage
  def redirectAction(request: RequestHeader): RedirectQuery = ()

  def getAppContextEndpoint = {
    getPageContext.implementedByAsync(
      new MuxHandlerAsyncHandler2[
        PageContext,
        ContextContent,
        UserInfo,
        SessionInfo,
      ] {
        def apply[R <: ContextContent](
            req: PageContext { type Response = R },
            userInfo: UserInfo,
            session: SessionInfo
        ): Future[R] = {

          def createProtectedContent = session
            .get("societyId")
            .map(
              societiesRepository
                .getSociety(_)
            ) match {
            case Some(value) =>
              value.map(maybeSociety =>
                ProtectedPageContent(userInfo, maybeSociety)
              )
            case None => {
              logger.info("societyId not found in session")
              Future.successful(ProtectedPageContent(userInfo, None))
            }
          }

          createProtectedContent.flatMap { implicit protectedContent =>
            req match {
              case FormResourcesContext =>
                allContextServices
                  .formResourcesContextService
                  .getContent
              case NoContext => Future.successful(EmptyContent())
              case RootPageContext =>
                allContextServices
                  .rootPageContextService
                  .getContent
              case SocietyRegistrationContext =>
                Future.successful(protectedContent)
              case SelectSocietyContext => {
                getUserSocieties(userInfo).map(societies =>
                  SelectSocietyContent(societies, protectedContent)
                )
              }
              case EventsContext => {
                Future.successful(EventsContent(Set(), protectedContent))
              }
            }
          }
        }
      }
    )
  }

  private def getUserSocieties(userInfo: UserInfo) = {
    val societyIds = userInfo
      .role
      .privileges
      .flatMap(_.scopes.get(Privilege.SOCIETIES_KEY).toSet.flatten)

    Future
      .sequence(societyIds.map(societiesRepository.getSociety))
      .map(_.map(_.toSet))
      .map(_.flatten)

  }

}
