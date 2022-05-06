package views

import com.raquo.waypoint.Route
import com.raquo.laminar.api.L._
import shared.pages.Page
import com.raquo.waypoint.Router
import scala.reflect.ClassTag
import com.raquo.waypoint.SplitRender
import components.BaseUIComponents
import org.scalajs.dom.document
import providers.ContextProvider
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import urldsl.language.dummyErrorImpl._
import shared.pages.content.ContextContent
import components.DisplayTopAppBarEvent

abstract class ViewImpl[Args, Context <: ContextContent](implicit
    val contextProvider: ContextProvider[Context]
) {
  type PageT <: Page
  implicit val tag: ClassTag[PageT]

  def route(): Route[PageT, Args]

  def render(pageT: Signal[PageT])(implicit
      router: Router[Page]
  ): HtmlElement

  def splitRender(
      sr: SplitRender[Page, HtmlElement]
  )(implicit
      router: Router[Page]
  ): SplitRender[Page, HtmlElement] =
    sr.collectSignal[PageT](render)
}

abstract class ViewImplWithNav[Args, Context <: ContextContent](
    override implicit val contextProvider: ContextProvider[Context]
) extends ViewImpl[Args, Context]() {

  implicit val uiComponents: BaseUIComponents
  val context: Var[Option[Context]] = Var(None)
  override def render(pageT: Signal[PageT])(implicit
      router: Router[Page]
  ): HtmlElement = {

    uiComponents.eventBus.emit(DisplayTopAppBarEvent(true))

    div(
      div(
        cls := "mdc-top-app-bar--fixed-adjust",
        renderContent(
          pageT
        )
      ),
      onMountBind(ctx =>
        contextProvider
          .getContext()
          .map(Some(_)) --> context
      )
    )
  }

  def renderContent(pageT: Signal[PageT])(implicit
      router: Router[Page]
  ): HtmlElement
}

abstract class ProtectedView[Args, Context <: ContextContent](
    override implicit val contextProvider: ContextProvider[Context]
) extends ViewImplWithNav[Args, Context]() {
  def canAccess(): Boolean
}
