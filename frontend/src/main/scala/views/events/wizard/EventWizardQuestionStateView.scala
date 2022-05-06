package views.events.wizard

import components.BaseUIComponents
import providers.ContextProvider
import shared.pages.content.ProtectedPageContent
import shared.pages.CreateEventPage
import scala.reflect.ClassTag
import views.ViewImplWithNav
import com.raquo.waypoint.Route
import urldsl.language.dummyErrorImpl._
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import components.DisplayFabEvent
import shared.pages.Page
import components.navigation.appbar.TopAppBar
import dao.events.EventWizardDescriptor
import dao.events.EventWizardQuestion
import components.input.radio.RadioButton
import services.EventWizardClient
import shared.pages.EventsPage
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import dao.events.NextQuestionResolver
import dao.events.JsonInputQuestionResolver
import dao.events.FormResourcesQuestionResolver
import dao.events.EventWizardQuestionResolver
import org.scalajs.dom.raw.Event
import dao.events.EventWizardState
import dao.events.EventWizardQuestionState
import io.circe.JsonObject
import services.FormResourceService
import dao.events.QuestionChoice
import shared.pages.EventWizardQuestionStatePage
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import shared.pages.EventWizardStatePage
import javax.swing.plaf.basic.BasicBorders.RadioButtonBorder
import shared.pages.EventWizardSubmitPage

class EventWizardQuestionStateView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[(String, String, String), ProtectedPageContent] {

  type PageT = EventWizardQuestionStatePage
  implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[EventWizardQuestionStatePage]
  )
  val nextPageBus = new EventBus[Page]

  def route(): Route[PageT, (String, String, String)] = Route(
    p => p.args,
    EventWizardQuestionStatePage.tupled(_),
    pattern = root / "event-wizard" / segment[String] / "state" / segment[
      String
    ] / "question" / segment[String] / endOfSegments
  )

  val radioButtonClickedBus = new EventBus[RadioButton]

  def renderContent(pageT: Signal[PageT])(implicit
      router: Router[Page]
  ): HtmlElement = {

    uiComponents.topAppBar.title.set("Plan an Event")
    uiComponents.eventBus.emit(DisplayFabEvent(false))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)

    div(
      cls := "content",
      child <-- pageT
        .map(_.args)
        .flatMap(args =>
          EventWizardClient
            .getEventWizardQuestionState(args._1, args._2, args._3)
            .collect({ case Some(v) => content(args._1, args._2, v) })
        ),
      nextPageBus --> { router.pushState(_) }
    )
  }

  private def content(
      wizardId: String,
      wizardStateId: String,
      eventWizardQuestionState: GetEventWizardQuestionState
  ) = {

    val selectedButton = eventWizardQuestionState.state.map(_.choice)

    def selectButton(value: String) =
      selectedButton.map(_ == value).getOrElse(false)

    val radioButtonMap = eventWizardQuestionState
      .question
      .options
      .map(entry => (RadioButton(entry._1, selectButton(entry._1)), entry._2))

    val radioButtons = radioButtonMap.keys.toSeq

    val radioButtonClicked = EventStream.mergeSeq(
      radioButtons.map(button =>
        button.events(onInput.mapToChecked.filter(_ == true)).mapTo(button)
      )
    )

    div(
      display("grid"),
      h1(eventWizardQuestionState.question.title),
      radioButtons,
      div(
        child <-- radioButtonClickedBus
          .events
          .map(r => radioButtonMap.find(_._1 == r))
          .collect({ case Some(value) => value })
          .map(entry =>
            renderResolver(
              wizardId,
              wizardStateId,
              entry._2,
              eventWizardQuestionState,
              entry._1
            )
          )
      ),
      radioButtonClicked --> radioButtonClickedBus,
      onMountCallback { _ =>
        selectedButton match {
          case Some(value) =>
            radioButtonClickedBus.emit(RadioButton(value, true))
          case None => {}
        }
      },
      radioButtonClickedBus.events --> { button =>
        EventWizardClient.saveEventWizardQuestionChoice(
          wizardId,
          wizardStateId,
          eventWizardQuestionState.question._id,
          button.labelText
        )
      },
      uiComponents
        .topAppBar
        .onBackButtonClicked
        .mapTo(EventWizardStatePage(wizardId, wizardStateId)) --> nextPageBus
    )

  }

  private def renderResolver(
      wizardId: String,
      wizardStateId: String,
      resolver: EventWizardQuestionResolver,
      eventWizardQuestionState: GetEventWizardQuestionState,
      radioButton: RadioButton
  ): HtmlElement = {

    val mapper = new QuestionResolverMapper(
      wizardId,
      wizardStateId,
      resolver,
      eventWizardQuestionState,
      radioButton
    ).editRoot(paddingBottom := "1rem")
    div(
      mapper,
      mapper.nextPageBus --> nextPageBus
    )

  }

  private def setDrawerHeader = contextProvider
    .getContext() --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))

}
