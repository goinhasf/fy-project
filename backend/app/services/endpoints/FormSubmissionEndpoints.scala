package services.endpoints
import endpoints4s.play.server
import com.google.inject.Inject
import services.PlayComponentsProvider
import clients.play.auth.AuthorizationClient
import shared.config.MicroServiceConfig
import services.EndpointService
import services.endpoints.handlers.SessionHandler
import services.endpoints.handlers.ActionHandlers
import services.endpoints.handlers.JwtValidator
import play.api.routing.Router
import services.impl.FormSubmissionsService
import dao.users.UserInfo
import dao.forms.SubmittedType
import dao.events.Reviewed
import dao.forms.ReviewedType
import dao.forms.InReviewType
import shared.endpoints.ChangeSubmissionStatus
import dao.forms.Submitted
import clients.play.users.UsersServiceClient
import clients.play.auth.AuthenticationClient

class FormSubmissionEndpoints @Inject() (
    pc: PlayComponentsProvider,
    override val authClient: AuthorizationClient,
    config: MicroServiceConfig,
    submissionService: FormSubmissionsService,
    usersService: AuthenticationClient
) extends EndpointService(
      pc
    )
    with shared.endpoints.FormSubmissionsEndpoints
    with server.JsonEntitiesFromCodecs
    with ActionHandlers
    with SessionHandler
    with JwtValidator {

  implicit val jwtConfig = config.jwtConfig

  def routes: Router.Routes = routesFromEndpoints(
    EndpointWithHandler2(
      getFormSubmission,
      defaultJwtValidator,
      extractOptValueFromSession("societyId"),
      submissionService.getFormSubmission
    ),
    EndpointWithHandler2(
      getSubmittedFormSubmissions,
      defaultJwtValidator,
      extractOptValueFromSession("societyId"),
      (_: Unit, userInfo: UserInfo, societyId: Option[String]) =>
        submissionService.getAllFormSubmissions(
          Seq(Submitted.getClass().getSimpleName().stripSuffix("$")),
          userInfo,
          societyId.getOrElse("")
        )
    ),
    EndpointWithHandler2(
      getReviewedFormSubmissions,
      defaultJwtValidator,
      extractOptValueFromSession("societyId"),
      (_: Unit, userInfo: UserInfo, societyId: Option[String]) =>
        submissionService.getAllFormSubmissions(
          Seq("Reviewed"),
          userInfo,
          societyId.getOrElse("")
        )
    ),
    EndpointWithHandler2(
      getInReviewFormSubmissions,
      defaultJwtValidator,
      extractOptValueFromSession("societyId"),
      (_: Unit, userInfo: UserInfo, societyId: Option[String]) =>
        submissionService.getAllFormSubmissions(
          Seq("InReview"),
          userInfo,
          societyId.getOrElse("")
        )
    ),
    EndpointWithHandler2(
      changeFormSubmissionStatus,
      defaultJwtValidator,
      extractOptValueFromSession("societyId"),
      (
          args: (String, ChangeSubmissionStatus, String),
          userInfo: UserInfo,
          societyId: Option[String]
      ) =>
        submissionService.changeFormSubmissionStatus(
          args._1,
          args._2,
          userInfo,
          societyId.getOrElse("")
        )
    ),
    EndpointWithHandler1(
      getUserName,
      defaultJwtValidator,
      (id: String, info: UserInfo) => {

        usersService
          .getUserInfoApiKey(
            id,
            config.apiKey.serviceId,
            config.apiKey.key
          )
          .collect({ case Some(value) =>
            s"${value.firstName} ${value.lastName}"
          })
      }
    )
  )
}
