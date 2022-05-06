package views.events
import components.BaseUIComponents
import views.ViewImplWithNav
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import urldsl.language.dummyErrorImpl._
import providers.ContextProvider
import shared.pages.content.ProtectedPageContent
import shared.pages.EventSubmissionDetailsPage
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.raw.HTMLElement
import shared.pages.Page
import components.DisplayFabEvent
import components.navigation.appbar.TopAppBar
import shared.endpoints.GetFormSubmission
import components.card.Card
import com.github.uosis.laminar.webcomponents.material.Textarea
import shared.endpoints.events.wizard.GetSocietyEvent
import com.github.uosis.laminar.webcomponents.material.Icon
import dao.events.UnderReview
import dao.events.Submitted
import dao.events.Reviewed
import services.SocietyEventsClient
import shared.pages.EventsPage
import dao.users.UserInfo
import com.github.uosis.laminar.webcomponents.material
import components.dialog.Dialog
import shared.auth.RegularUserRole
import shared.pages.AdminEventSubmissionsPage
import dao.events.SocietyEventStatus
import dao.events.SocietyEvent
import components.card.ActionableCard
import components.card.CardActionButton
import dao.forms
class EventSubmissionDetailsView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[String, ProtectedPageContent] {
  type PageT = EventSubmissionDetailsPage
  implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[EventSubmissionDetailsPage]
  )

  def route(): Route[PageT, String] = Route(
    p => p.args,
    EventSubmissionDetailsPage(_),
    pattern = root / "events" / segment[String] / endOfSegments
  )

  def renderContent(pageT: Signal[EventSubmissionDetailsPage])(implicit
      router: Router[Page]
  ): ReactiveHtmlElement[HTMLElement] = {
    uiComponents.topAppBar.title.set("Event Submission Details")
    uiComponents.eventBus.emit(DisplayFabEvent(false))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)
    div(
      cls := "content",
      child <-- pageT.flatMap(args =>
        SocietyEventsClient
          .getSocietyEventDetails(args.id)
          .collect({ case Some(v) => v })
          .flatMap(v =>
            contextProvider
              .getContext()
              .map(_.userInfo)
              .map(user => renderFromSubmissionCards(v, user))
          )
      )
    )
  }

  def renderFromSubmissionCards(
      eventDetails: GetSocietyEvent,
      userInfo: UserInfo
  )(implicit router: Router[Page]) = {

    val notesEl = span(
      if (eventDetails.event.societyEventStatus.status == "Reviewed") {
        eventDetails.event.societyEventStatus.asInstanceOf[Reviewed].notes
      } else ""
    )

    val textNotesSection = div(
      display.none,
      margin("1rem"),
      h3("Reviewer's Notes"),
      div(notesEl)
    )

    val dialog = new Dialog {
      val reviewNotes =
        Textarea(_.label("Notes")).amend(display.flex, marginBottom("1rem"))
      val selectEl = material
        .Select(
          _.label("Decision"),
          _.slots.default(material.List.ListItem(_.selected(true))),
          _.slots.default(
            createSelectItem(
              "Approved",
              eventDetails.event.societyEventStatus
            )
          ),
          _.slots.default(
            createSelectItem(
              "Changes Requested",
              eventDetails.event.societyEventStatus
            )
          )
        )
        .amend(display.flex)
      val content: HtmlElement = {
        div(
          selectEl,
          reviewNotes,
          material
            .Button(_.label("Submit"))
            .amend(onClick --> { _ => mdcComponent.now().map(_.close("")) })
            .amend(
              onMountBind(ctx =>
                onClick --> { _ =>
                  SocietyEventsClient
                    .reviewEvent(
                      eventDetails.event._id,
                      Reviewed(
                        reviewNotes.ref.value,
                        selectEl.ref.value == "Changes Requested"
                      )
                    )
                    .foreach { id =>
                      router.pushState(AdminEventSubmissionsPage())
                    }(ctx.owner)
                }
              )
            )
        )
      }
      val contentDescription: String = "Review the event"
      val title: String              = "Add Review to Event Submission"
    }

    def showReviewButton = eventDetails
      .formSubmissions
      .forall(
        _.submission.formSubmissionStatus.isInstanceOf[forms.Reviewed]
      ) && (eventDetails.event.societyEventStatus match {
      case Reviewed(notes, changes) => changes
      case _                        => true
    })

    val reviewCardContent = div(
      textNotesSection,
      div(
        display {
          if (userInfo.role.roleType == RegularUserRole()) {
            "none"
          } else {
            "flex"
          }
        },
        material
          .Button(
            _.label("Add Review"),
            _.disabled(!showReviewButton)
          )
          .amend(onClick --> { _ =>
            dialog.mdcComponent.now().map(_.open())
          }),
        dialog
      )
    )

    val headerCard = new Card(
      s"Event ${eventDetails.event._id}",
      eventDetails.event.details.description
    )

    def getReviewStatus = eventDetails.event.societyEventStatus.status

    val reviewInfoCard = new Card(
      s"Review Status - ${getReviewStatus}",
      "Your reviewer will add any necessary notes here",
      Some(reviewCardContent)
    ) {
      override def headerElement(): HtmlElement = div(
        cls := "mdc-card-wrapper__text-section",
        div(
          cls := "mdc-card__title",
          Icon(
            _.slots.default(span(eventDetails.event.societyEventStatus match {
              case UnderReview() => "group"
              case Submitted()   => "outbox"
              case Reviewed(notes, requiresChanges) => {
                notesEl.amend(value(notes))
                textNotesSection.amend(display("grid"))
                if (requiresChanges) "error" else "done"
              }
            }))
          ).amend(opacity(0.6)),
          span(child.text <-- title.signal)
        ),
        div(cls := "mdc-card__subtitle", child.text <-- subTitle.signal)
      )
    }
    val formsList = EventSubmissionFormsList(eventDetails.formSubmissions)
    val formSubmissionsCard = new Card(
      "Form Submissions",
      "This card displays the uploaded event forms review status",
      Some(formsList)
    )

    def showFacebookCard =
      userInfo.role.roleType == RegularUserRole() && (eventDetails
        .event
        .societyEventStatus match {
        case Reviewed(notes, requiresChanges) => requiresChanges == false
        case _                                => false
      })

    val facebookCard = new ActionableCard(
      "Facebook",
      "Add this event to facebook",
      None,
      List(CardActionButton("Add"))
    ).amend(display {
      if (showFacebookCard) { "flex" }
      else "None"
    })

    div(
      headerCard,
      formSubmissionsCard,
      reviewInfoCard,
      facebookCard,
      formsList.elementClickBus --> { router.pushState(_) },
      uiComponents.topAppBar.onBackButtonClicked --> { _ =>
        if (userInfo.role.roleType == RegularUserRole()) {
          router.pushState(EventsPage())
        } else {
          router.pushState(AdminEventSubmissionsPage())
        }
      }
    )

  }
  private def selectItem(value: String, status: SocietyEventStatus) =
    status match {
      case Reviewed(notes, requiresChanges) => {
        requiresChanges && value == "Changes Requested"
      }
      case _ => false
    }

  private def createSelectItem(value: String, status: SocietyEventStatus) =
    material
      .List
      .ListItem(
        _.slots.default(span(value)),
        _.value(value),
        _.selected { selectItem(value, status) }
      )
}
