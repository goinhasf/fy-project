package views.admin.submissions
import components.BaseUIComponents
import providers.ContextProvider
import views.ViewImplWithNav
import shared.pages.content.ProtectedPageContent
import com.raquo.laminar.api.L._
import urldsl.language.dummyErrorImpl._
import shared.pages.AdminEventSubmissionsPage
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.Router
import org.scalajs.dom.raw.HTMLElement
import shared.pages.Page
import dao.events.SocietyEvent
import components.card.Card
import com.github.uosis.laminar.webcomponents.material
import shared.endpoints.events.wizard.GetSocietyEvent
import dao.societies.Society
import shared.pages.EventSubmissionDetailsPage
import services.SocietyEventsClient
import shared.endpoints.events.wizard.SocietyEventsEndpoints
import components.DisplayTopAppBarEvent
import com.github.uosis.laminar.webcomponents.material.TopAppBar
import components.navigation.appbar
import dao.events.Reviewed

class EventSubmissionsView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[Unit, ProtectedPageContent]() {
  type PageT = AdminEventSubmissionsPage
  implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[AdminEventSubmissionsPage]
  )

  def route(): Route[PageT, Unit] = Route[PageT, Unit](
    _ => (),
    _ => AdminEventSubmissionsPage(),
    root / "admin" / "event-submissions" / endOfSegments
  )

  def renderContent(pageT: Signal[AdminEventSubmissionsPage])(implicit
      router: Router[Page]
  ): ReactiveHtmlElement[HTMLElement] = {
    uiComponents.eventBus.emit(DisplayTopAppBarEvent(true))
    uiComponents.topAppBar.title.set("Event Submissions")
    uiComponents.topAppBar.primaryButton.set(appbar.TopAppBar.MenuButton)

    div(
      cls := "content",
      child <-- SocietyEventsClient
        .getSocietyEvents(SocietyEventsEndpoints.SubmittedQ)
        .flatMap(seq =>
          SocietyEventsClient
            .getSocietyEvents(SocietyEventsEndpoints.ReviewedQ)
            .map(
              _.filter(t =>
                t.event.societyEventStatus match {
                  case Reviewed(notes, requiresChanges) => requiresChanges == false
                  case _ => false
                }
              )
            )
            .map(_ ++ seq)
        )
        .map(renderEventSubmissions),
      setDrawerHeader
    )
  }

  def renderEventSubmissions(seq: Seq[GetSocietyEvent])(implicit
      router: Router[Page]
  ) = {

    val list = seq.foldRight(material.List())((event, list) =>
      list.amend(material.List.slots.default(createListItem(event)))
    )

    new Card(
      "Society Event Submissions",
      "This is a list of all events that have been submitted for approval",
      Some(list)
    )
  }

  def createListItem(
      getSocietyEvent: GetSocietyEvent
  )(implicit router: Router[Page]) = {

    val societyEvent = getSocietyEvent.event
    material
      .List
      .ListItem(
        _.slots.default(
          span(societyEvent.details.name, marginRight("0.25rem")),
          span(opacity(0.6), societyEvent._id)
        ),
        _.slots.secondary(span(getSocietyEvent.society.details.name))
      )
      .amend(onClick.mapTo(EventSubmissionDetailsPage(societyEvent._id)) --> {
        router.pushState(_)
      })
  }
  private def setDrawerHeader = contextProvider
    .getContext() --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))
}
