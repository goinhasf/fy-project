package views.events.wizard

import dao.events.EventWizardQuestionResolver
import dao.events.NextQuestionResolver
import dao.events.JsonInputQuestionResolver
import dao.events.FormResourcesQuestionResolver
import com.raquo.laminar.api.L._
import components.input.text.OutlinedTextField
import services.FormResourceService
import io.circe.JsonObject
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import org.scalajs.dom.document
import dao.forms.FormResourceFieldDescriptor
import dao.forms.SingleFormResourceFieldDescriptor
import dao.forms.LoopFormResourceFieldDescriptor
import views.formResourceDetails.SingleFormResourceFieldComponent
import views.formResourceDetails.LoopFormResourceFieldComponent
import views.formResourceDetails.FormResourceFieldComponent
import dao.forms.FormResourceFields
import dao.forms.FormResource
import components.MDCComponent
import components.MaterialComponent
import com.raquo.laminar.keys.ReactiveEventProp
import org.scalajs._
import scalajs.js._
import scalajs.js.Dynamic.global
import org.scalajs.dom.document
import org.scalajs.dom.raw._
import org.scalajs.dom.experimental.HttpMethod
import io.circe.syntax._
import io.circe.Json
import Utils._
import com.raquo.waypoint.Router
import shared.pages.Page
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import shared.pages.EventWizardSubmitPage
import shared.pages.EventWizardQuestionStatePage
import services.EventWizardClient
import dao.events.QuestionChoice
import components.input.radio.RadioButton

class QuestionResolverMapper(
    wizardId: String,
    wizardStateId: String,
    resolver: EventWizardQuestionResolver,
    eventWizardQuestionState: GetEventWizardQuestionState,
    radioButton: RadioButton
) extends MaterialComponent[MDCComponent] {
  implicit val nextPageBus: EventBus[Page] = new EventBus[Page]
  val state: Var[JsonObject]               = Var(JsonObject.empty)

  protected val rootElement: HtmlElement =
    resolve(resolver, eventWizardQuestionState.state.flatMap(_.data))
  def resolve(
      resolver: EventWizardQuestionResolver,
      questionState: Option[JsonObject] = None
  ): HtmlElement =
    resolver match {
      case nextQuestionResolver: NextQuestionResolver =>
        mapNextQuestionResolver(nextQuestionResolver, questionState)
      case jsonInputResolver: JsonInputQuestionResolver =>
        mapJsonInputResolver(jsonInputResolver, questionState)
      case formResourcesResolver: FormResourcesQuestionResolver =>
        mapFormResourceQuestionResolver(formResourcesResolver, questionState)
    }

  def mapJsonInputResolver(
      jsonInput: JsonInputQuestionResolver,
      questionState: Option[JsonObject]
  ) = {
    val nextQuestionButton = MaterialButton(
      TextButtonLabel("Next"),
      ButtonStyles.raisedButtonStyle
    ).editRoot(
      onClick
        .mapTo(resolver.nextQuestionId)
        .collect({ case Some(v) => v })
        .map(qId =>
          EventWizardQuestionStatePage(wizardId, wizardStateId, qId)
        ) --> nextPageBus
    ).editRoot(onClick --> { _ =>
      EventWizardClient.saveEventWizardQuestionState(
        wizardId,
        wizardStateId,
        eventWizardQuestionState.question._id,
        QuestionChoice(radioButton.labelText, Some(state.now()))
      )
    }).editRoot(
      onClick
        .mapTo(resolver.nextQuestionId)
        .collect({ case None =>
          EventWizardSubmitPage(wizardId, wizardStateId)
        }) --> nextPageBus
    )

    val inputs = jsonInput
      .descriptors
      .map[FormResourceFieldComponent] {
        _ match {
          case s: SingleFormResourceFieldDescriptor =>
            new SingleFormResourceFieldComponent(
              s,
              questionState.map(adjustCursor(Seq(jsonInput.key), _))
            ).editRoot { root =>
              root.state --> { s =>
                state.update(current =>
                  current.deepMerge(
                    JsonObject(jsonInput.key -> s.asJson)
                  )
                )
              }
            }
          case l: LoopFormResourceFieldDescriptor =>
            new LoopFormResourceFieldComponent(
              l,
              questionState
            ).editRoot { root =>
              root.isEditingVar.set(true)
              root.state --> { s =>
                state.update(current =>
                  current.deepMerge(
                    JsonObject(jsonInput.key -> s.asJson)
                  )
                )
              }
            }
        }
      }

    div(
      h2("Data Required"),
      div(
        cls := "edit-grid",
        inputs.map(entry =>
          div(
            div(
              cls := "edit-grid-item",
              entry
            )
          )
        )
      ),
      nextQuestionButton
    )

  }

  def mapNextQuestionResolver(
      nextQuestionResolver: NextQuestionResolver,
      questionState: Option[JsonObject]
  ) = {

    val nextQuestionButton = MaterialButton(
      TextButtonLabel("Next"),
      ButtonStyles.raisedButtonStyle
    ).editRoot(
      onClick
        .mapTo(resolver.nextQuestionId)
        .collect({ case Some(v) => v })
        .map(qId =>
          EventWizardQuestionStatePage(wizardId, wizardStateId, qId)
        ) --> nextPageBus
    ).editRoot(onClick --> { _ =>
      EventWizardClient.saveEventWizardQuestionState(
        wizardId,
        wizardStateId,
        eventWizardQuestionState.question._id,
        QuestionChoice(radioButton.labelText, Some(state.now()))
      )
    }).editRoot(
      onClick
        .mapTo(resolver.nextQuestionId)
        .collect({ case None =>
          EventWizardSubmitPage(wizardId, wizardStateId)
        }) --> nextPageBus
    )
    div(
      nextQuestionResolver
        .extras
        .foldRight(div())((entry, htmlEl) =>
          htmlEl.amend(h2(entry._1), p(entry._2))
        ),
      nextQuestionButton
    )
  }

  def mapFormResourceQuestionResolver(
      formResourceQuestionResolver: FormResourcesQuestionResolver,
      questionState: Option[JsonObject]
  ) = {
    val nextQuestionButton = MaterialButton(
      TextButtonLabel("Next"),
      ButtonStyles.raisedButtonStyle
    ).editRoot(
      onClick
        .mapTo(resolver.nextQuestionId)
        .collect({ case Some(v) => v })
        .map(qId =>
          EventWizardQuestionStatePage(wizardId, wizardStateId, qId)
        ) --> nextPageBus
    ).editRoot(
      onClick
        .mapTo(resolver.nextQuestionId)
        .collect({ case None =>
          EventWizardSubmitPage(wizardId, wizardStateId)
        }) --> nextPageBus
    )
    div(
      FormResourceQuestionResolver(
        wizardId,
        wizardStateId,
        formResourceQuestionResolver,
        eventWizardQuestionState
      ),
      nextQuestionButton
    )
  }
}

object QuestionResolverMapper {}
