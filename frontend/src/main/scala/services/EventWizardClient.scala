package services

import shared.endpoints.events.wizard.EventsWizardEndpoints

import endpoints4s.xhr
import dao.events.EventWizardQuestionState
import services.util.DefaultCsrfTokenConsumer
import dao.events.QuestionChoice

object EventWizardClient
    extends XHRObservableEndpoints
    with EventsWizardEndpoints
    with ClientService
    with DefaultCsrfTokenConsumer
    with xhr.JsonEntitiesFromCodecs {

  def saveEventWizardQuestionState(
      eventWizardId: String,
      wizardStateId: String,
      questionId: String,
      choice: QuestionChoice
  ) = super.saveEventWizardQuestionState(
    eventWizardId,
    wizardStateId,
    questionId,
    choice,
    getCsrfToken().get
  )

  def saveEventWizardQuestionChoice(
      eventWizardId: String,
      wizardStateId: String,
      questionId: String,
      choice: String
  ) = super.saveEventWizardQuestionChoice(
    eventWizardId,
    wizardStateId,
    questionId,
    choice,
    getCsrfToken().get
  )

  def startNewEventWizard(eventWizardId: String) = super
    .startNewEventWizard(
      eventWizardId,
      getCsrfToken().get
    )

  def submitEventWizard(eventWizardId: String, wizardStateId: String) =
    super.submitEventWizard(
      eventWizardId,
      wizardStateId,
      getCsrfToken().get
    )

  def deleteEventWizardState(eventWizardId: String, wizardStateId: String) =
    super.deleteEventWizardState(
      eventWizardId,
      wizardStateId,
      getCsrfToken().get
    )
}
