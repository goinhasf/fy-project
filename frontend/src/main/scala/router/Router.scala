package router
import scala.scalajs.js.constructorOf

import com.raquo.laminar.api.L._
import com.raquo.waypoint._
import components.AutoInit
import components.BaseUIComponents
import components.MDCDrawer
import components.MDCTopAppBar
import components.navigation.appbar.TopAppBar
import components.navigation.drawer.NavigationDrawer
import org.scalajs.dom

import views.ui.AppNavigationDrawer
import views.formResourceDetails.FormResourcePageImpl
import views.formResources.FormResourcesView
import shared.pages.FormResourcePage
import shared.pages.FormResourcesPage
import shared.pages.Page
import shared.pages.RootPage
import views.ViewImpl
import views.root.RootPageImpl
import components.button.FloatingActionButton
import components.button.MaterialButton
import views.login.LoginPageImpl
import providers.ContextProvider
import shared.pages.NoContext
import scala.concurrent.Future
import providers.PageContextProviders
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import views.ui.PageNavigationClickEvent
import components.DisplayTopAppBarEvent
import views.registerSociety.SocietyRegistrationView
import views.selectSociety.SelectSocietyView
import views.events.EventsView
import views.events.wizard.EventWizardQuestionStateView
import views.events.wizard.EventWizardStateView
import views.events.wizard.EventWizardSubmissionView
import views.events.wizard.EventWizardFormResourceView
import views.login.AdminLoginView
import views.admin.AdminMainView
import views.admin.submissions.FormSubmissionDetailsView
import views.events.EventSubmissionDetailsView
import views.admin.submissions.EventSubmissionsView

object ApplicationRouter {

  implicit val uiComponents: BaseUIComponents = {

    val topBar = TopAppBar(
      "App Bar Title"
    )

    val drawer = new AppNavigationDrawer().editRoot(thisEl =>
      topBar.eventBus.events --> thisEl.appBarEventsObserver
    )

    val fab = FloatingActionButton()
    BaseUIComponents(topBar, drawer, fab)
  }

  val pages = {
    import PageContextProviders._

    List[ViewImpl[_, _]](
      new FormResourcesView(),
      new FormResourcePageImpl(),
      new RootPageImpl(),
      new LoginPageImpl(),
      new SocietyRegistrationView(),
      new SelectSocietyView(),
      new EventsView(),
      new EventWizardQuestionStateView(),
      new EventWizardStateView(),
      new EventWizardSubmissionView(),
      new EventWizardFormResourceView(),
      new AdminLoginView(),
      new AdminMainView(),
      new FormSubmissionDetailsView(),
      new EventSubmissionDetailsView(),
      new EventSubmissionsView()
    )
  }

  val routes = pages.map(_.route())

  implicit val waypointRouter: Router[Page] = new Router[Page](
    routes = routes,
    getPageTitle = _.toString,
    serializePage = page => page.asJson.noSpaces,
    deserializePage = pageStr =>
      decode[Page](pageStr)
        .getOrElse(throw new Exception("Error decoding page"))
  )(
    windowEvents.onPopState,
    unsafeWindowOwner,
    initialUrl = dom.document.location.href,
    origin = dom.document.location.origin.get
  )

  val pageSplitter: SplitRender[Page, HtmlElement] = pages
    .foldRight(
      SplitRender[Page, HtmlElement](waypointRouter.$currentPage)
    )(_ splitRender _)

  def loadApp(): Subscription = {
    documentEvents
      .onDomContentLoaded
      .foreach(_ => {
        val root = dom.document.getElementById("app-root")
        render(
          root,
          div(
            uiComponents.topAppBar,
            uiComponents.drawer,
            div(cls := "mdc-drawer-scrim"),
            child <-- pageSplitter.$view,
            uiComponents.fab,
            uiComponents.drawer.onPageChange --> waypointRouter.pushState _
          )
        )
      })(unsafeWindowOwner)
  }
}
