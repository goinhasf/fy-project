package views.events.wizard
import com.raquo.laminar.api.L._
import urldsl.language.dummyErrorImpl._
import components.BaseUIComponents
import views.ViewImplWithNav
import providers.ContextProvider
import shared.pages.content.ProtectedPageContent
import shared.pages.EventWizardQuestionFormPage
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.Router
import org.scalajs.dom.raw.HTMLElement
import shared.pages.Page
import urldsl.language.Tupler4
import urldsl.language.Tupler
import dao.forms.FormResource
import io.circe.JsonObject
import io.circe.syntax._
import views.formResourceDetails.FormResourceFieldComponent
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import org.scalajs.dom.document
import services.FormResourceService
import services.EventWizardClient
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import shared.endpoints.events.wizard.ops.GetEventWizardState
import dao.events.QuestionChoice
import shared.pages.EventWizardQuestionStatePage
import components.DisplayFabEvent
import components.navigation.appbar.TopAppBar
import io.circe.ACursor
import io.circe.Json

class EventWizardFormResourceView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[
      ((String, String, String), String),
      ProtectedPageContent
    ] {

  type PageT = EventWizardQuestionFormPage
  implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[EventWizardQuestionFormPage]
  )

  def route() = Route[PageT, ((String, String, String), String)](
    pattern = root / "event-wizard" / segment[String] / "state" / segment[
      String
    ] / "question" / segment[String] / "form" / segment[
      String
    ] / endOfSegments,
    decode = args => {
      val (arg1, arg2, arg3) = args._1
      EventWizardQuestionFormPage.tupled(arg1, arg2, arg3, args._2)
    },
    encode = p => ((p.args._1, p.args._2, p.args._3), p.args._4)
  )

  val state: Var[JsonObject] = Var(JsonObject.empty)

  def renderContent(pageT: Signal[EventWizardQuestionFormPage])(implicit
      router: Router[Page]
  ): HtmlElement = {

    uiComponents.topAppBar.title.set("Plan an Event - Form Resource")
    uiComponents.eventBus.emit(DisplayFabEvent(false))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)

    div(
      cls := "content",
      child <-- pageT
        .map(_.args)
        .flatMap(args =>
          for {
            eventWizardQuestionState <- EventWizardClient
              .getEventWizardQuestionState(args._1, args._2, args._3)
              .collect({ case Some(v) => v })
            formResource <- FormResourceService
              .getResource(args._4)
              .collect({ case Some(v) => v })
          } yield content(
            args._1,
            args._2,
            formResource,
            eventWizardQuestionState
          )
        ),
      setDrawerHeader
    )
  }

  def content(
      wizardId: String,
      wizardStateId: String,
      resource: FormResource,
      questionState: GetEventWizardQuestionState
  )(implicit router: Router[Page]): HtmlElement = {

    import Utils._

    val adjustedOverallState = {
      val json = questionState.overallState.asJson
      val keys = json.hcursor.keys.toSeq.flatten
      keys
        .map(json.hcursor.get[Json](_).getOrElse(JsonObject.empty.asJson))
        .foldRight(JsonObject.empty.asJson)(_ deepMerge _)
        .asObject
        .getOrElse(JsonObject.empty)
    }

    println(adjustedOverallState)
    println(questionState.overallState)


    val answers = questionState
      .state
      .flatMap(_.data)
      .map(state => adjustCursor(Seq("forms", resource._id), state))
      .orElse(
        resource
          .defaultFieldValues
          .map(state => adjustCursor(Seq("forms", resource._id), state))
          .map(_ deepMerge (adjustedOverallState))
      )
      .orElse(Some(adjustedOverallState))

    val formFields = FormResourceFieldComponent
      .createInputsFromJsonObject(
        resource.fields,
        answers
      )
      .map(root =>
        root.editRoot(root.state --> { s =>
          state.update(current =>
            current.deepMerge(
              JsonObject(
                "forms" -> JsonObject(resource._id -> s.asJson).asJson
              )
            )
          )
        })
      )

    div(
      cls := "event-form-section",
      div(
        cls := "form-header",
        div(
          cls := "form-header--title",
          h2(s"${resource.details.name}"),
          p(s"${resource.details.notes}")
        ),
        MaterialButton(
          TextButtonLabel("Preview"),
          ButtonStyles.outlinedButtonStyle
        ).editRoot { el =>
          onMountBind[HtmlElement](ctx =>
            onClick --> { _ =>
              EventWizardClient
                .saveEventWizardQuestionState(
                  wizardId,
                  wizardStateId,
                  questionState.question._id,
                  QuestionChoice(
                    questionState.state.get.choice,
                    Some(state.now())
                  )
                )
                .foreach { _ =>
                  document.location.href = FormResourceService
                    .getFormResourceAsPdf(
                      resource._id,
                      Some(
                        FormResourceFieldComponent.getJsonObjectFromFields(
                          formFields
                        )
                      )
                    )
                }(ctx.owner)
            }
          )
        }.editRoot(cls := "form-header--button")
      ),
      div(
        cls := "edit-grid",
        formFields.map(_.editRoot(cls := "edit-grid-item"))
      ),
      MaterialButton(
        TextButtonLabel("Save and Return"),
        ButtonStyles.raisedButtonStyle
      ).editRoot(
        onMountBind[HtmlElement](ctx =>
          onClick --> { _ =>
            EventWizardClient
              .saveEventWizardQuestionState(
                wizardId,
                wizardStateId,
                questionState.question._id,
                QuestionChoice(
                  questionState.state.get.choice,
                  Some(state.now())
                )
              )
              .foreach { _ =>
                router.pushState(
                  EventWizardQuestionStatePage(
                    wizardId,
                    wizardStateId,
                    questionState.question._id
                  )
                )
              }(ctx.owner)
          }
        )
      ),
      uiComponents
        .topAppBar
        .onBackButtonClicked
        .mapTo(
          EventWizardQuestionStatePage(
            wizardId,
            wizardStateId,
            questionState.question._id
          )
        ) --> { router.pushState(_) }
    )
  }
  private def setDrawerHeader = contextProvider
    .getContext() --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))

}
