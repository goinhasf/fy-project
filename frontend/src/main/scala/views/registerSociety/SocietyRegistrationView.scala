package views.registerSociety

import components.BaseUIComponents
import providers.ContextProvider
import shared.pages.content.ProtectedPageContent
import views.ViewImplWithNav
import shared.pages.SocietyRegistrationPage
import scala.reflect.ClassTag
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import shared.pages.Page
import components.DisplayFabEvent
import components.navigation.appbar.TopAppBar
import com.raquo.waypoint.Route
import urldsl.language.dummyErrorImpl._
import views.registerSociety.form.SocietyRegistrationForm
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import shared.pages.SelectSocietyPage
import components.form.FormResultReceived

class SocietyRegistrationView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[Unit, ProtectedPageContent]() {
  override type PageT = SocietyRegistrationPage
  override implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[SocietyRegistrationPage]
  )

  val registrationForm = new SocietyRegistrationForm()

  override def renderContent(
      pageT: Signal[PageT]
  )(implicit
      router: Router[Page]
  ): HtmlElement = {
    uiComponents.eventBus.emit(DisplayFabEvent(false))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)
    uiComponents.topAppBar.title.set("Register Your Society")

    div(
      cls := "content",
      h1("Enter your Society's Details"),
      registrationForm
    ).amend(
      onMountBind(ctx => setDrawerHeader),
      registrationForm
        .eventBus
        .events
        .collect({ case FormResultReceived(r) => r })
        .collect({ case Right(v) => v }) --> { _ =>
        router.pushState(SelectSocietyPage())

      },
      uiComponents
        .topAppBar
        .onBackButtonClicked
        .mapTo(SelectSocietyPage()) --> (router.pushState(_))
    )
  }

  private def setDrawerHeader = contextProvider
    .getContext() --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))

  override def route(): Route[PageT, Unit] = Route(
    encode = page => (),
    decode = _ => SocietyRegistrationPage(),
    pattern = root / "register-your-society" / endOfSegments
  )
}
