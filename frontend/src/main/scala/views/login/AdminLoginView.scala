package views.login

import com.raquo.laminar.api.L._
import urldsl.language.dummyErrorImpl._
import providers.ContextProvider
import components.BaseUIComponents
import views.ViewImplWithNav
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import shared.pages.content.EmptyContent
import shared.pages.AdminLoginPage
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.Router
import org.scalajs.dom.raw.HTMLElement
import shared.pages.Page
import components.DisplayTopAppBarEvent
import components.DisplayFabEvent
import org.scalajs.dom
import services.AdminPagesEndpoints
import components.form.FormResultReceived

class AdminLoginView(
    override implicit val contextProvider: ContextProvider[EmptyContent],
    override implicit val uiComponents: BaseUIComponents
) extends ViewImplWithNav[Unit, EmptyContent] {

  type PageT = AdminLoginPage

  implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[AdminLoginPage]
  )

  def route(): Route[PageT, Unit] = Route(
    encode = page => (),
    decode = _ => AdminLoginPage(),
    pattern = root / "admin" / "login" / endOfSegments
  )

  val loginForm = LoginForm()
  def renderContent(pageT: Signal[AdminLoginPage])(implicit
      router: Router[Page]
  ): ReactiveHtmlElement[HTMLElement] = {

    uiComponents.eventBus.emit(DisplayTopAppBarEvent(false))
    uiComponents.eventBus.emit(DisplayFabEvent(false))

    def handleResult(result: Option[String]) = {
      result match {
        case Some(value) => {
          dom
            .document
            .location
            .pathname_=(AdminPagesEndpoints.adminMainPage.href(()))
        }
        case None => {
          println("Login failed.")
          EventStream.empty
        }
      }
    }

    LoginCard(loginForm).amend(
      loginForm
        .eventBus
        .events
        .collect({ case FormResultReceived(result) => result })
        --> (handleResult(_))
    )

  }
}
