package views.events.wizard

import components.BaseUIComponents
import providers.ContextProvider
import shared.pages.content.ProtectedPageContent
import views.ViewImplWithNav
import shared.pages.EventWizardStatePage
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import urldsl.language.dummyErrorImpl._
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.Router
import org.scalajs.dom.raw.HTMLElement
import shared.pages.Page
import shared.endpoints.events.wizard.ops.GetEventWizardState
import services.EventWizardClient
import components.button.MaterialButton
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import components.list.item.MaterialListItem
import components.list.item.builders.ListItemTextElement
import components.list.item.builders.EmptyBeforeListElement
import components.list.item.builders.AfterListItemIcon
import components.list.MaterialList
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import shared.pages.EventWizardQuestionStatePage
import components.DisplayFabEvent
import components.navigation.appbar.TopAppBar
import shared.pages.EventsPage
import shared.pages.EventWizardSubmitPage

class EventWizardStateView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[(String, String), ProtectedPageContent] {

  type PageT = EventWizardStatePage
  implicit val tag: ClassTag[PageT] = ClassTag(classOf[EventWizardStatePage])

  val nextPageBus = new EventBus[Page]

  def route(): Route[PageT, (String, String)] = Route(
    p => p.args,
    EventWizardStatePage.tupled(_),
    pattern = root / "event-wizard" / segment[String] / "state" / segment[
      String
    ] / endOfSegments
  )
  def renderContent(pageT: Signal[EventWizardStatePage])(implicit
      router: Router[Page]
  ): HtmlElement = {

    uiComponents.topAppBar.title.set("Plan an Event")
    uiComponents.eventBus.emit(DisplayFabEvent(false))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)

    div(
      cls := "content",
      child <-- pageT
        .map(_.args)
        .flatMap(EventWizardClient.getEventWizardState)
        .collect({ case Some(value) => value })
        .map(content),
      nextPageBus --> (router.pushState(_)),
      uiComponents
        .topAppBar
        .onBackButtonClicked
        .mapTo(EventsPage()) --> nextPageBus,
      setDrawerHeader
    )
  }

  def content(eventWizardState: GetEventWizardState): HtmlElement = {

    val listOfQuestions = WizardQuestionsList(eventWizardState)
    val buttonLabel = {
      if (
        listOfQuestions
          .state
          .questions
          .flatMap(_.state.toSeq)
          .length == eventWizardState.questions.length
      ) {
        "Submit"
      } else {
        "Resume"
      }
    }

    val resumeButton: MaterialButton =
      MaterialButton(
        TextButtonLabel(buttonLabel),
        ButtonStyles.raisedButtonStyle
      )
        .editRoot(
          onClick
            .mapTo(findFirstIncompleteQuestion(eventWizardState.questions))
            .collect({ case Some(v) => v })
            .map(question =>
              EventWizardQuestionStatePage(
                eventWizardState.state._descId,
                eventWizardState.state._id,
                question.question._id
              )
            ) --> nextPageBus
        )
        .editRoot(root =>
          onClick
            .filter(_ =>
              root
                .buttonLabel
                .label
                .now()
                .map(_ == "Submit")
                .getOrElse(false)
            )
            .mapTo(
              EventWizardSubmitPage(
                eventWizardState.state._descId,
                eventWizardState.state._id
              )
            ) --> nextPageBus
        )

    div(
      span(
        cls("form-header"),
        h1(
          marginRight := "1rem",
          overflow.hidden,
          textOverflow.ellipsis,
          s"Event Draft #${eventWizardState.state._id}"
        ),
        resumeButton.editRoot(margin.auto)
      ),
      listOfQuestions,
      listOfQuestions.elementClickBus --> nextPageBus
    )
  }

  private def findFirstIncompleteQuestion(
      seq: Seq[GetEventWizardQuestionState]
  ) = seq.filter(_.state.isEmpty).headOption

  private def setDrawerHeader = contextProvider
    .getContext() --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))
}
