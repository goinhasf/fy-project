package services.endpoints

import com.google.inject._
import endpoints4s.play.server.PlayComponents
import endpoints4s.play.server
import services.EndpointService
import play.api.routing.Router
import shared.endpoints.events.wizard.EventsWizardEndpoints
import services.endpoints.handlers.ActionHandlers
import services.endpoints.handlers.SessionHandler
import data.repositories.EventWizardRepository
import data.repositories.EventWizardStateRepository
import data.repositories.SocietyEventsRepository
import services.endpoints.handlers.JwtValidator
import play.api.mvc.Results
import shared.config.MicroServiceConfig
import scala.concurrent.Future
import dao.users.UserInfo
import clients.play.auth.AuthorizationClient
import dao.events.EventWizardQuestionState
import io.circe.Json
import io.circe.syntax._
import io.circe.JsonObject
import dao.events.SocietyEvent
import org.bson.types.ObjectId
import dao.events.SocietyEventDetails
import data.repositories.FormResourceRepository
import dao.events.QuestionChoice
import data.repositories.FormSubmissionsRepository
import dao.forms.FormSubmission
import dao.forms.Submitted
import java.time.Instant
import java.util.Date
import endpoints4s.Invalid
import dao.events.EventWizardQuestion
import dao.events.NextQuestionResolver
import dao.events.JsonInputQuestionResolver
import dao.events.FormResourcesQuestionResolver
import views.html.helper.form
import io.circe.Decoder
import io.circe.HCursor
import shared.utils.DateFormatting
import dao.events.SocietyEventType
import dao.events.Repeat
import java.util.concurrent.TimeUnit
import play.api.Logger
import shared.endpoints.events.wizard.ops.GetEventWizardState
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import dao.events.Frequency
import services.impl.EventWizardSubmitServiceImpl
import data.repositories.SocietiesRepository

@Singleton
class EventWizardEndpoints @Inject() (
    pc: PlayComponents,
    microServiceConfig: MicroServiceConfig,
    eventWizardService: EventWizardSubmitServiceImpl,
    override val authClient: AuthorizationClient
) extends ServerAuthentication(microServiceConfig, authClient, pc)
    with EventsWizardEndpoints
    with server.JsonEntitiesFromCodecs
    with ActionHandlers
    with JwtValidator
    with SessionHandler {

  def routes: Router.Routes = routesFromEndpoints(
    getAllEventTypesEndpoint,
    getEventWizardForEventTypeEndpoint,
    startNewEventWizardEndpoint,
    getEventWizardStateEndpoint,
    getEventWizardQuestionStateEndpoint,
    saveEventWizardQuestionStateEndpoint,
    saveQuestionChoiceEndpoint,
    getDraftEventWizardStatesEndpoint,
    submitEventWizardEndpoint,
    deleteEventWizardStateEndpoint
  )

  def getAllEventTypesEndpoint = EndpointWithHandler1(
    getAllEventTypes,
    defaultJwtValidator,
    (_: Unit, _: UserInfo) => {
      eventWizardService.getAllEventTypes()
    }
  )

  def startNewEventWizardEndpoint = EndpointWithHandler2(
    startNewEventWizard,
    defaultJwtValidator,
    extractValueFromSession("societyId"),
    eventWizardService.startEventWizard
  )

  def getDraftEventWizardStatesEndpoint = EndpointWithHandler2(
    getDraftEventWizardStates,
    defaultJwtValidator,
    extractValueFromSession("societyId"),
    (_: Unit, userInfo: UserInfo, societyId: String) =>
      eventWizardService.getDraftEventWizardStates(societyId)
  )

  def getEventWizardForEventTypeEndpoint = EndpointWithHandler1(
    getEventWizardForEventType,
    defaultJwtValidator,
    (eventType: String, _: UserInfo) =>
      eventWizardService.getEventWizardForEventType(eventType)
  )
  def getEventWizardStateEndpoint = EndpointWithHandler2(
    getEventWizardState,
    defaultJwtValidator,
    extractValueFromSession("societyId"),
    eventWizardService.getEventWizardState
  )

  def getEventWizardQuestionStateEndpoint = EndpointWithHandler2(
    getEventWizardQuestionState,
    defaultJwtValidator,
    extractValueFromSession("societyId"),
    (args: (String, String, String), user: UserInfo, socId: String) => {
      val (wizardId, stateId, questionId) = args

      eventWizardService.getEventWizardQuestionState(
        wizardId,
        stateId,
        questionId,
        user,
        socId
      )

    }
  )

  def saveQuestionChoiceEndpoint = EndpointWithHandler1(
    saveEventWizardQuestionChoice,
    defaultJwtValidator,
    (args: (String, String, String, String, String), _: UserInfo) => {
      val (wizardId, stateId, questionId, choice, _) = args
      eventWizardService.saveQuestionChoice(stateId, questionId, choice)
    }
  )

  def saveEventWizardQuestionStateEndpoint = EndpointWithHandler1(
    saveEventWizardQuestionState,
    defaultJwtValidator,
    (args: (String, String, String, QuestionChoice, String), _: UserInfo) => {
      val (wizardId, stateId, questionId, choice, _) = args
      eventWizardService.saveEventWizardQuestionState(
        stateId,
        questionId,
        choice
      )
    }
  )

  def submitEventWizardEndpoint = EndpointWithHandler2(
    submitEventWizard,
    defaultJwtValidator,
    extractValueFromSession("societyId"),
    (args: (String, String, String), userInfo: UserInfo, societyId: String) => {
      eventWizardService.submitEventWizardService(args, userInfo, societyId)
    }
  )

  def deleteEventWizardStateEndpoint = EndpointWithHandler1(
    deleteEventWizardState,
    defaultJwtValidator,
    (args: (String, String, String), _: UserInfo) =>
      eventWizardService
        .deleteEventWizardState(args._2)
        .map(_.map(_ => ()))
  )

}
