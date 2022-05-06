package views.admin.submissions

import components.BaseUIComponents
import providers.ContextProvider
import views.ViewImplWithNav
import shared.pages.content.ProtectedPageContent
import com.raquo.laminar.api.L._
import urldsl.language.dummyErrorImpl._
import shared.pages.FormSubmissionDetailsPage
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.Router
import org.scalajs.dom.raw.HTMLElement
import shared.pages.Page
import dao.forms.FormSubmission
import components.DisplayTopAppBarEvent
import shared.endpoints.GetFormSubmission
import dao.forms.FormResource
import views.formResourceDetails.EditFormResourceCard
import components.card.CardActionButton
import services.FormResourceService
import components.card.ActionableCard
import dao.forms.Ready
import dao.forms.Submitted
import dao.forms.InReview
import dao.forms.Reviewed
import services.FormSubmissionsClient
import dao.forms.FormSubmissionStatus
import shared.utils.DateFormatting
import components.input.switch.Switch
import com.github.uosis.laminar.webcomponents.material.Textarea
import components.card.Card
import org.scalajs.dom.document
import components.navigation.appbar.TopAppBar
import shared.pages.AdminMainPage
import com.github.uosis.laminar.webcomponents.material.Dialog
import com.github.uosis.laminar.webcomponents.material
import moment.Moment
import dao.users.UserInfo
import shared.endpoints.ChangeSubmissionStatus
import shared.endpoints.ChangeToReviewed
import shared.auth.RegularUserRole
import org.scalajs.dom.window

class FormSubmissionDetailsView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[String, ProtectedPageContent]() {
  type PageT = FormSubmissionDetailsPage
  implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[FormSubmissionDetailsPage]
  )

  def route(): Route[PageT, String] = Route[PageT, String](
    _.args,
    FormSubmissionDetailsPage(_),
    root / "form-submissions" / segment[String] / endOfSegments
  )

  val formSubmissionEventBus = new EventBus[GetFormSubmission]

  def renderContent(pageT: Signal[FormSubmissionDetailsPage])(implicit
      router: Router[Page]
  ): ReactiveHtmlElement[HTMLElement] = {
    uiComponents.eventBus.emit(DisplayTopAppBarEvent(true))
    uiComponents.topAppBar.title.set("Form Submission Details")
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)
    div(
      cls := "content",
      child <-- formSubmissionEventBus
        .events
        .combineWith(contextProvider.getContext())
        .flatMap(args =>
          getNamesMap(args._1.submission.formSubmissionStatus)
            .map(m => renderFormSubmission(args._1, args._2.userInfo, m))
        ),
      setDrawerHeader,
      onMountBind(ctx =>
        pageT
          .flatMap(t => FormSubmissionsClient.getFormSubmission(t.args))
          .collect({ case Some(v) => v }) --> formSubmissionEventBus
      )
    )
  }

  def getNamesMap(submission: FormSubmissionStatus) = submission match {
    case Ready() => EventStream.fromValue(Map[String, String]())
    case Submitted(_submittedById, submissionDate) =>
      FormSubmissionsClient
        .getUserName(_submittedById)
        .map(name => Map(_submittedById -> name))
    case InReview(
          _submittedById,
          _reviewerId,
          submissionDate,
          reviewStartDate
        ) =>
      FormSubmissionsClient
        .getUserName(_submittedById)
        .map(name => Map(_submittedById -> name))
        .flatMap(map =>
          FormSubmissionsClient
            .getUserName(_reviewerId)
            .map(name => map ++ Map(_reviewerId -> name))
        )
    case Reviewed(
          _submittedById,
          _reviewerId,
          submissionDate,
          reviewStartDate,
          reviewEndDate,
          changesRequested,
          notes
        ) =>
      FormSubmissionsClient
        .getUserName(_submittedById)
        .map(name => Map(_submittedById -> name))
        .flatMap(map =>
          FormSubmissionsClient
            .getUserName(_reviewerId)
            .map(name => map ++ Map(_reviewerId -> name))
        )
  }

  def renderFormSubmission(
      formSubmission: GetFormSubmission,
      userInfo: UserInfo,
      map: Map[String, String]
  )(implicit router: Router[Page]) = {
    val editFormResourceCard = new EditFormResourceCard(
      formSubmission.formResource,
      userInfo
    )

    def renderStatus = formSubmission.submission.formSubmissionStatus match {
      case Ready() => "Ready to submit"
      case Submitted(_submittedById, submissionDate) =>
        s"Submitted by ${map(_submittedById)}"
      case InReview(
            _submittedById,
            _reviewerId,
            submissionDate,
            reviewStartDate
          ) =>
        s"In Review by ${map(_reviewerId)}"
      case Reviewed(
            _submittedById,
            _reviewerId,
            submissionDate,
            reviewStartDate,
            reviewEndDate,
            changesRequested,
            notes
          ) =>
        s"Reviewed by ${map(_reviewerId)}"
    }

    def enableReview = {

      if (userInfo.role.roleType == RegularUserRole()) {
        false
      } else {

        formSubmission.submission.formSubmissionStatus match {
          case submitted: Submitted => true
          case _                    => false
        }
      }
    }

    def renderDates = formSubmission.submission.formSubmissionStatus match {
      case Ready() => span(renderStatus)
      case Submitted(_submittedById, submissionDate) =>
        div(
          cls := "edit-grid",
          b(renderStatus, marginRight("0.25rem")),
          div(
            b("Submission Date:", marginRight("0.25rem")),
            span(opacity(0.6), DateFormatting.longToString(submissionDate))
          )
        )
      case InReview(
            _submittedById,
            _reviewerId,
            submissionDate,
            reviewStartDate
          ) =>
        div(
          cls := "edit-grid",
          div(
            b("Submission Date:", marginRight("0.25rem")),
            span(opacity(0.6), DateFormatting.longToString(submissionDate))
          ),
          div(
            b("Review Start Date:", marginRight("0.25rem")),
            span(opacity(0.6), DateFormatting.longToString(reviewStartDate))
          )
        )
      case Reviewed(
            _submittedById,
            _reviewerId,
            submissionDate,
            reviewStartDate,
            reviewEndDate,
            changesRequested,
            notes
          ) =>
        div(
          cls := "edit-grid",
          div(
            b("Submission Date:", marginRight("0.25rem")),
            span(opacity(0.6), DateFormatting.longToString(submissionDate))
          ),
          div(
            b("Review Start Date:", marginRight("0.25rem")),
            span(opacity(0.6), DateFormatting.longToString(reviewStartDate))
          ),
          div(
            b("Review End Date:", marginRight("0.25rem")),
            span(opacity(0.6), DateFormatting.longToString(reviewEndDate))
          ),
          div(
            b("Reviewed by:", marginRight("0.25rem")),
            span(opacity(0.6), map(_reviewerId))
          )
        )
    }
    val selectedItem = Var("Approve")

    val isReviewing = Var(false)

    val isReviewed = formSubmission.submission.formSubmissionStatus match {
      case r: Reviewed => {
        selectedItem.set {
          if (r.changesRequested) {
            "Request Changes"
          } else {
            "Approve"
          }
        }
        true
      }
      case _ => false
    }

    val reviewButton = CardActionButton("Review")
      .editRoot(root => onClick.mapTo(!isReviewing.now()) --> isReviewing)
      .editRoot(root =>
        isReviewing.signal.map {
          case true  => Some("Submit")
          case false => Some("Review")
        } --> root.buttonLabel.label
      )
      .editRoot(visibility := { if (enableReview) "block" else "none" })

    val textArea =
      Textarea(_.label("Review Notes"), _.disabled(!enableReview))
        .amend(display.flex, padding("0.5rem"))

    val infoCard = new Card(
      s"Form Submission ${formSubmission.submission._id}",
      s"Submitted by ${formSubmission.society.details.name} Society"
    )

    val dialogReviewButton = material.Button(_.icon("send"), _.label("Submit"))

    val reviewDecisionMenu = material
      .Select(
        _.disabled(!enableReview),
        _.onSelected.mapToValue --> selectedItem,
        _.value("Approve"),
        _.required(true),
        _.label("Decision"),
        _.slots.default(
          material
            .List
            .ListItem(
              _.selected(true)
            )
        ),
        _.slots.default(
          material
            .List
            .ListItem(
              _.slots.default(span("Approve")),
              _.value("Approve")
            )
        ),
        _.slots.default(
          material
            .List
            .ListItem(
              _.slots.default(span("Request Changes")),
              _.value("Request Changes")
            )
        )
      )
      .amend(display.flex, padding("0.5rem"))

    val primaryActionButton = material
      .Button(_.label("Confirm"))

    val secondaryActionButton = material
      .Button(_.label("Cancel"))

    def closeOnClick(el: HtmlElement, dialog: HtmlElement) =
      el.events(onClick) --> { _ =>
        dialog.amend(Dialog.open(false))
      }

    def primaryButtonOnClick =
      primaryActionButton.events(onClick).mapTo(selectedItem.now())

    val confirmationDialog = Dialog(
      _.heading("Confirm Review"),
      _.slots.default(span(s"You've selected ${selectedItem.now()}")),
      dialog =>
        dialog
          .slots
          .primaryAction(primaryActionButton),
      dialog =>
        dialog
          .slots
          .secondaryAction(secondaryActionButton)
    ).amendThis(thisEl =>
      primaryButtonOnClick --> { value =>
        println(value)
        FormSubmissionsClient
          .changeFormSubmissionStatus(
            formSubmission.submission._id,
            ChangeToReviewed(
              userInfo.id,
              if (value == "Approve") false else true,
              Option.when(!textArea.ref.value.isEmpty())(textArea.ref.value)
            )
          )
          .collect({ case Some(value) => AdminMainPage() }) --> {
          router.pushState(_)
        }
      }
    ).amendThis(thisEl => closeOnClick(secondaryActionButton, thisEl))
      .amendThis(thisEl => closeOnClick(primaryActionButton, thisEl))

    val reviewCard = new ActionableCard(
      "Review Details",
      "Enter notes and decision",
      Some(
        div(
          renderDates,
          textArea,
          reviewDecisionMenu
        )
      ),
      List(
        new CardActionButton("Review")
          .editRoot(onClick --> { _ =>
            confirmationDialog.amend(Dialog.open(true))
          })
          .editRoot(
            display(
              if (isReviewed || userInfo.role.roleType == RegularUserRole())
                "none"
              else "block"
            )
          )
      )
    )

    editFormResourceCard
      .actions
      .set(
        List(
          CardActionButton("Preview Form").editRoot(
            onClick
              .mapTo(formSubmission.submission.values) --> { obj =>
              document.location.href = FormResourceService
                .getFormResourceAsPdf(
                  formSubmission.formResource._id,
                  Some(obj)
                )
            }
          )
        )
      )

    div(
      infoCard,
      editFormResourceCard,
      reviewCard,
      confirmationDialog,
      uiComponents.topAppBar.onBackButtonClicked --> { _ =>
        if (userInfo.role.roleType == RegularUserRole()) {
          window.history.back()
        } else {
          router.pushState(AdminMainPage())
        }
      }
    )
  }

  private def setDrawerHeader = contextProvider
    .getContext() --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))
}
