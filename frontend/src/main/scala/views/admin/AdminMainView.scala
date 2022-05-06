package views.admin
import com.raquo.laminar.api.L._
import urldsl.language.dummyErrorImpl._
import providers.ContextProvider
import components.BaseUIComponents
import views.ViewImplWithNav
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import shared.pages.content.ProtectedPageContent
import com.raquo.waypoint.Router
import shared.pages.Page
import shared.pages.AdminMainPage
import components.DisplayTopAppBarEvent
import dao.forms.FormSubmission
import components.card.ActionableCard
import com.github.uosis.laminar.webcomponents.material
import shared.endpoints.GetFormSubmission
import dao.forms.Ready
import dao.forms.Submitted
import dao.forms.InReview
import dao.forms.Reviewed
import shared.utils.DateFormatting
import scala.scalajs.js.Date
import views.admin.submissions.SubmissionUtils
import services.FormSubmissionsClient
import shared.pages.FormSubmissionDetailsPage
import components.navigation.appbar.TopAppBar
import javax.smartcardio.CardTerminal
import components.card.Card
class AdminMainView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[Unit, ProtectedPageContent]() {

  type PageT = AdminMainPage
  implicit val tag: ClassTag[PageT] = ClassTag(classOf[AdminMainPage])

  def route(): Route[PageT, Unit] = Route[PageT, Unit](
    _ => (),
    _ => AdminMainPage(),
    root / "admin" / endOfSegments
  )

  def renderContent(pageT: Signal[PageT])(implicit
      router: Router[Page]
  ): HtmlElement = {
    uiComponents.eventBus.emit(DisplayTopAppBarEvent(true))
    uiComponents.topAppBar.title.set("Admin Centre")
    uiComponents.topAppBar.primaryButton.set(TopAppBar.MenuButton)

    div(
      cls := "content",
      div(
        child <-- FormSubmissionsClient
          .getSubmittedFormSubmissions()
          .map(
            createFormSubmissionsCard(
              "Form Submissions",
              "Here you can action and review societies form submissions"
            )
          )
      ),
      div(
        child <-- FormSubmissionsClient
          .getReviewedFormSubmissions()
          .map(
            createFormSubmissionsCard(
              "Forms Reviewed",
              "This shows all forms that have been reviewed by admins"
            )
          )
      ),
      setDrawerHeader
    )
  }

  private def createFormSubmissionsCard(title: String, subtitle: String)(
      submissions: Seq[GetFormSubmission]
  )(implicit router: Router[Page]): HtmlElement = {

    val list = submissions.foldRight(material.List())((sub, list) =>
      list.amend(
        material
          .List
          .slots
          .default(
            SubmissionUtils
              .submissionListItemFactory(sub)
              .amend(
                onClick
                  .mapTo(
                    FormSubmissionDetailsPage(sub.submission._id)
                  ) --> { router.pushState(_) }
              )
          )
      )
    )

    new Card(
      title, {
        if (submissions.isEmpty) "Nothing yet"
        else subtitle
      },
      { if (submissions.isEmpty) None else Some(list) }
    )
  }

  private def setDrawerHeader = contextProvider
    .getContext() --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))

}
