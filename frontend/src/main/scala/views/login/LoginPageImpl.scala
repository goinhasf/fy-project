package views.login

import views.ViewImpl
import shared.pages.LoginPage
import scala.reflect.ClassTag
import com.raquo.waypoint.Route
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import urldsl.language.dummyErrorImpl._
import components.form.FormResultReceived
import shared.pages.Page
import shared.pages.RootPage
import providers.ContextProvider
import shared.pages.content.EmptyContent
import org.scalajs.dom
import services.PageEndpoints
import org.scalajs.dom.experimental.domparser.DOMParser
import org.scalajs.dom.experimental.domparser.SupportedType
import views.ViewImplWithNav
import components.BaseUIComponents
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.raw.HTMLElement
import components.DisplayTopAppBarEvent
import components.DisplayFabEvent

class LoginPageImpl(
    override implicit val contextProvider: ContextProvider[EmptyContent],
    override implicit val uiComponents: BaseUIComponents
) extends ViewImplWithNav[Unit, EmptyContent] {

  type PageT = LoginPage

  implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[LoginPage]
  )

  def route(): Route[PageT, Unit] = Route(
    encode = page => (),
    decode = _ => LoginPage(),
    pattern = root / "login" / endOfSegments
  )

  val loginForm = LoginForm()

  override def renderContent(
      pageT: Signal[LoginPage]
  )(implicit router: Router[Page]): ReactiveHtmlElement[HTMLElement] = {

    uiComponents.eventBus.emit(DisplayTopAppBarEvent(false))
    uiComponents.eventBus.emit(DisplayFabEvent(false))

    def handleResult(result: Option[String]) = {
      result match {
        case Some(value) => {
          dom
            .document
            .location
            .pathname_=(PageEndpoints.societySelectionPage.href(()))
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
