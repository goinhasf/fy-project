package services.endpoints
import com.google.inject._
import shared.config.MicroServiceConfig
import play.api.mvc.DefaultControllerComponents
import clients.play.auth.AuthorizationClient
import play.api.http.DefaultHttpErrorHandler
import play.api.routing.Router
import views.SinglePageAppView
import services.endpoints.handlers.ActionHandlers
import play.filters.csrf.CSRF
import pdi.jwt.JwtClaim
import scala.concurrent.Future
import play.api.mvc.{Handler, RequestHeader}
import services.endpoints.handlers.JwtValidator
import services.endpoints.handlers.CsrfHandler
import views.AppTemplate
import endpoints4s.play.server.PlayComponents
import services.endpoints.handlers.SessionHandler
import dao.users.UserInfo

@Singleton
class PagesEndpointService @Inject() (
    config: MicroServiceConfig,
    authorizationClient: AuthorizationClient,
    playComponents: PlayComponents
) extends ServerAuthentication(
      config,
      authorizationClient,
      playComponents
    )
    with HtmlEndpoints
    with shared.endpoints.pages.PageEndpoints
    with ActionHandlers
    with SessionHandler
    with JwtValidator
    with CsrfHandler {

  val authClient: AuthorizationClient = authorizationClient

  def routes: Router.Routes = routesFromEndpoints(
    loginPageEndpoint,
    rootPageEndpoints,
    formResourcesPageEndpoint,
    formResourceEndpoint,
    societyRegistrationEndpoint,
    societySelectionEndpoint,
    submitSocietySelectionEndpoint,
    eventsPageEndpoint,
    eventWizardStartPageEndpoint,
    eventWizardStatePageEndpoint,
    eventWizardQuestionStatePageEndpoint,
    eventWizardQuestionFormPageEndpoint,
    eventWizardSubmissionPageEndpoint,
    eventSubmissionDetailsPageEndpoint,
    formSubmissionDetailsPageEndpoint
  )

  def redirectAction = redirect(loginPage)(()).apply(())

  def protectedRouteHandler[A](
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
      },
      (
          responseObj: ResponseObject[(A, CSRF.Token), Option[HtmlPage]],
          requestHeader: RequestHeader
      ) => {
        requestHeader.session.get("societyId") match {
          case Some(value) => Future.successful(responseObj.result)
          case None        => Future.successful(redirectAction)
        }
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

  def loginPageEndpoint = EndpointWithHandler1[Unit, AppTemplate, CSRF.Token](
    loginPage,
    csrfExtractor,
    (_, token) =>
      Future.successful(
        SinglePageAppView
          .getSinglePageAppView("Society Management", token)
      )
  )

  def submitSocietySelectionEndpoint = {

    val combinedRequestHandler =
      actionHandlerUnion(csrfExtractor, sessionJwtValidator(redirectAction))(
        (csrf, string) => csrf
      )

    EndpointWithHandler1[
      (String, String),
      Unit,
      String,
    ](
      submitSocietySelection,
      sessionJwtValidator(redirectAction),
      (societyId, _) => {
        Future.successful(())
      },
      (
          responseObject: ResponseObject[((String, String), String), Unit],
          reqHeader: RequestHeader
      ) =>
        Future.successful(
          responseObject
            .result
            .withSession(
              reqHeader.session + ("societyId" -> responseObject
                .requestArgs
                ._1
                ._1)
            )
        )
    )
  }

  def rootPageEndpoints         = protectedRouteHandler(rootPage)
  def formResourcesPageEndpoint = protectedRouteHandler(formResourcesPage)
  def formResourceEndpoint      = protectedRouteHandler(formResourcePage)
  def societyRegistrationEndpoint = protectedRouteHandler(
    societyRegistrationPage
  )
  def societySelectionEndpoint = createPlayHandler(societySelectionPage)
  def eventsPageEndpoint       = protectedRouteHandler(eventsPage)

  def eventWizardStartPageEndpoint = protectedRouteHandler(eventWizardStartPage)
  def eventWizardStatePageEndpoint = protectedRouteHandler(eventWizardStatePage)
  def eventWizardQuestionStatePageEndpoint = protectedRouteHandler(
    eventWizardQuestionStatePage
  )
  def eventWizardQuestionFormPageEndpoint = protectedRouteHandler(
    eventWizardQuestionFormPage
  )
  def eventWizardSubmissionPageEndpoint = protectedRouteHandler(
    eventWizardSubmissionPage
  )
  def eventSubmissionDetailsPageEndpoint = protectedRouteHandler(
    eventSubmissionDetailsPage
  )

  def formSubmissionDetailsPageEndpoint = protectedRouteHandler(
    formSubmissionDetails
  )

}
