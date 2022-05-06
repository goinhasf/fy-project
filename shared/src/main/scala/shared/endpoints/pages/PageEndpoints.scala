package shared.endpoints.pages

import shared.endpoints.HtmlEndpoints
import shared.endpoints.authentication.Authentication
import endpoints4s.algebra
import endpoints4s.Tupler
import shared.endpoints.SecurityExtensions
trait PageEndpoints
    extends HtmlEndpoints
    with SecurityExtensions
    with LoginEndpoint {

  private def createAppPageEndpoint[U, UCookie](
      endpointPath: Url[U]
  )(implicit tuplerUCookie: Tupler.Aux[U, String, UCookie]) = endpoint(
    get(
      endpointPath
    ),
    sessionResponse(pageResponse)
  )

  def rootPage          = createAppPageEndpoint(path)
  def formResourcesPage = createAppPageEndpoint(path / "form-resources")
  def formResourcePage = createAppPageEndpoint(
    path / "form-resources" / segment[String]("resource-id")
  )
  def societyRegistrationPage = createAppPageEndpoint(
    path / "register-your-society"
  )
  def societySelectionPage = createAppPageEndpoint(
    path / "select-society"
  )
  def submitSocietySelection = endpoint(
    csrfPost(
      path / "select-society",
      textRequest,
      emptyRequestHeaders
    ),
    ok(emptyResponse)
  )

  def eventsPage = createAppPageEndpoint(path / "events")

  def eventWizardStartPage = createAppPageEndpoint(
    path / "event-wizard" / segment[String]("eventWizardId") / "new"
  )

  def eventWizardStatePage = createAppPageEndpoint(
    path / "event-wizard" / segment[String](
      "eventWizardId"
    ) / "state" / segment[String]("eventWizardStateId")
  )
  def eventWizardQuestionStatePage = createAppPageEndpoint(
    path / "event-wizard" / segment[String](
      "eventWizardId"
    ) / "state" / segment[String]("eventWizardStateId") / "question" / segment[
      String
    ]("questionId")
  )

  def eventWizardQuestionFormPage = createAppPageEndpoint(
    path / "event-wizard" / segment[String](
      "eventWizardId"
    ) / "state" / segment[String]("eventWizardStateId") / "question" / segment[
      String
    ]("questionId") / "form" / segment[String]("formId")
  )

  def eventWizardSubmissionPage = createAppPageEndpoint(
    path / "event-wizard" / segment[String](
      "eventWizardId"
    ) / "state" / segment[String]("eventWizardStateId") / "submit"
  )

  def formSubmissionDetails = createAppPageEndpoint(
    path / "form-submissions" / segment[String]("id")
  )

  def eventSubmissionDetailsPage = createAppPageEndpoint(
    path / "events" / segment[String]("id")
  )
}
