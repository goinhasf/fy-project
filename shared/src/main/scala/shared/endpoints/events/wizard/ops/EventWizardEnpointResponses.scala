package shared.endpoints.events.wizard.ops

import dao.events.EventWizardQuestionState
import dao.events.EventWizardQuestion
import dao.events.EventWizardState
import dao.events.QuestionChoice
import io.circe.generic.JsonCodec
import io.circe.JsonObject

@JsonCodec
case class GetEventWizardState(
    state: EventWizardState,
    questions: Seq[GetEventWizardQuestionState]
)
@JsonCodec
case class GetEventWizardQuestionState(
    question: EventWizardQuestion,
    state: Option[QuestionChoice],
    overallState: JsonObject
)
