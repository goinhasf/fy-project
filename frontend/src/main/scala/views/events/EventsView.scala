package views.events

import components.BaseUIComponents
import providers.ContextProvider
import shared.pages.content.EventsContent
import views.ViewImplWithNav
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import shared.pages.Page
import shared.pages.EventsPage
import urldsl.language.dummyErrorImpl._
import components.navigation.appbar.TopAppBar
import components.DisplayFabEvent
import shared.pages.CreateEventPage
import components.dialog.Dialog
import components.{MDCComponent, MaterialComponent}
import components.id.Identifiable
import components.list.item.MaterialListItem
import dao.events.SocietyEventType
import components.input.radio.RadioButton
import components.id.ComponentID
import services.EventWizardClient
import shared.pages.EventWizardStatePage
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import services.SocietyEventsClient
import shared.endpoints.events.wizard.SocietyEventsEndpoints

class EventsView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[EventsContent]
) extends ViewImplWithNav[Unit, EventsContent] {

  type PageT = EventsPage
  implicit val tag: ClassTag[PageT] = ClassTag(classOf[EventsPage])

  def route(): Route[PageT, Unit] = Route(
    p => (),
    _ => EventsPage(),
    pattern = root / "events" / endOfSegments
  )

  def renderContent(pageT: Signal[PageT])(implicit
      router: Router[Page]
  ): HtmlElement = {

    val dialog: Dialog = new Dialog {
      val contentDescription: String =
        "Select the type of event you wish to plan"
      val title: String = "Create a new event"
      val content: HtmlElement = div(
        child <-- EventWizardClient
          .getAllEventTypes()
          .map(EventTypeRadioButtonGroup.apply)
          .map { radioButtonGroup =>
            div(
              radioButtonGroup,
              MaterialButton(
                TextButtonLabel("Start"),
                ButtonStyles.raisedButtonStyle
              ).editRoot(radioButtonGroup.onRadioButtonChecked --> {
                args: (String, String) =>
                  router.pushState(EventWizardStatePage(args._1, args._2))
              })
            )

          }
      )

    }

    uiComponents.topAppBar.title.set("Events Center")
    uiComponents.eventBus.emit(DisplayFabEvent(true))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.MenuButton)

    div(
      cls := "content",
      setDrawerHeader,
      div(
        child <-- EventWizardClient
          .getDraftEventWizardStates()
          .map(DraftEventsCard.apply)
      ),
      div(
        child <-- SocietyEventsClient
          .getSocietyEvents(SocietyEventsEndpoints.SubmittedQ)
          .map(events =>
            SubmittedEventsCard.apply(
              "Submitted Events",
              "Here you can see the events that have already been submitted for approval",
              events
            )
          )
      ),
      div(
        child <-- SocietyEventsClient
          .getSocietyEvents(SocietyEventsEndpoints.ReviewedQ)
          .map(events =>
            SubmittedEventsCard.apply(
              "Reviewed Events",
              "Here you can see the events that have already been reviewed",
              events
            )
          )
      ),
      dialog,
      uiComponents.fab.events(onClick) --> { _ =>
        dialog.mdcComponent.now().map(_.open())
      }
    )
  }

  private def setDrawerHeader = contextProvider
    .getContext()
    .map(_.protectedInfo) --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))

}

case class EventTypeRadioButtonGroup(options: Seq[SocietyEventType])
    extends MaterialComponent[MDCComponent]
    with Identifiable {
  val id: ComponentID = ComponentID("mdc-radio-group")

  private val radioButtons: Seq[RadioButton] = options
    .map(typ => RadioButton(typ.name, false))

  protected val rootElement: HtmlElement = div(
    cls := "edit-grid",
    radioButtons
  )

  val onRadioButtonChecked = EventStream
    .mergeSeq(
      radioButtons.map(el =>
        el.getValue().signal.changes.collect { case true =>
          el
        }
      )
    )
    .flatMap(radio =>
      EventWizardClient
        .getEventWizardForEventType(radio.labelText)
        .collect({ case Some(value) => value._id })
    )
    .flatMap(descId =>
      EventWizardClient
        .startNewEventWizard(descId)
        .map(stateId => (descId, stateId))
    )
}
