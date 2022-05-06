package shared.endpoints.events.wizard

import endpoints4s.algebra
import shared.endpoints.SecurityExtensions
import dao.events.EventWizardQuestion
import io.circe.Json
import dao.events.EventWizardQuestionState
import dao.events.EventWizardState
import dao.events.QuestionChoice
import dao.events.EventWizardDescriptor
import shared.endpoints.events.wizard.ops.GetEventWizardState
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import dao.events.SocietyEventType

trait EventsWizardEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with SecurityExtensions {

  def restPath = path / "api" / "event-wizard"

  def getAllEventTypes = endpoint(
    get(restPath / "event-types"),
    ok(jsonResponse[Seq[SocietyEventType]])
  )

  def getEventWizardForEventType = endpoint(
    get(restPath / segment[String]("eventType")),
    ok(jsonResponse[EventWizardDescriptor]).orNotFound()
  )

  /** @return Returns the id of the created wizard state
    */
  def startNewEventWizard = endpoint(
    csrfPost(
      restPath / segment[String]("descId") / "new",
      emptyRequest,
      emptyRequestHeaders
    ),
    ok(jsonResponse[String])
  )

  def getEventWizardState = endpoint(
    get(
      restPath / segment[String]("eventWizardId") / "state" / segment[String](
        "wizardStateId"
      )
    ),
    ok(jsonResponse[GetEventWizardState]).orNotFound()
  )

  def getDraftEventWizardStates = endpoint(
    get(
      restPath / "state" / "drafts"
    ),
    ok(jsonResponse[Seq[EventWizardState]])
  )

  def getEventWizardQuestionState = endpoint(
    get(
      restPath / segment[String]("eventWizardId") / "state" / segment[String](
        "stateId"
      ) / "question" / segment[String]("questionId")
    ),
    ok(jsonResponse[GetEventWizardQuestionState]).orNotFound()
  )

  def saveEventWizardQuestionState = endpoint(
    csrfPost(
      restPath / segment[String]("eventWizardId") / "state" / segment[String](
        "stateId"
      ) / "question" / segment[String]("questionId"),
      jsonRequest[QuestionChoice],
      emptyRequestHeaders
    ),
    ok(emptyResponse).orNotFound()
  )

  def saveEventWizardQuestionChoice = endpoint(
    csrfPut(
      restPath / segment[String]("eventWizardId") / "state" / segment[String](
        "stateId"
      ) / "question" / segment[String]("questionId"),
      textRequest,
      emptyRequestHeaders
    ),
    ok(emptyResponse).orNotFound()
  )

  /** @return Returns the id of the created society event if event wizard is in valid state
    */
  def submitEventWizard = endpoint(
    csrfPost(
      restPath / segment[String]("eventWizardId") / "state" / segment[String](
        "wizardStateId"
      ) / "submit",
      emptyRequest,
      emptyRequestHeaders
    ),
    badRequest().orElse(ok(textResponse))
  )

  def deleteEventWizardState = endpoint(
    csrfDelete(
      restPath / segment[String]("eventWizardId") / "state" / segment[String](
        "wizardStateId"
      ),
      emptyRequestHeaders,
      None
    ),
    ok(emptyResponse).orNotFound()
  )

}
