package components.navigation.drawer

import com.raquo.laminar.api.L._
import components.MDCDrawer
import components.MaterialComponent
import components.navigation.drawer.NavigationDrawerEvents
import org.scalajs.dom
import scalajs.js
import components.navigation.appbar.AppBarEvents
import components.navigation.appbar.PrimaryButtonClicked
import components.navigation.appbar.ActionButtonClicked
import components.list.item.MaterialListItem
import components.list.MaterialList
import org.scalajs.dom.raw.Event
import com.raquo.laminar.builders.HtmlTag
import components.navigation.appbar.MenuButton
import components.MDCComponent
import components.card.Card

class NavigationDrawerHeader(title: String, subtitle: String)
    extends MaterialComponent[MDCComponent] {

  val titleElement    = h3(cls := "mdc-drawer__title", title)
  val subtitleElement = h6(cls := "mdc-drawer__subtitle", subtitle)

  protected val rootElement: HtmlElement = new Card(
    title,
    subtitle
  ).editRoot(
    cls := "mdc-drawer__header mdc-card__primary-action mdc-card--outlined",
    margin := "0"
  )

}

class NavigationDrawer(
    itemsList: Seq[NavigationDrawerItem] = List()
) extends MaterialComponent[MDCDrawer] {

  val materialList                     = new NavigationDrawerList(itemsList)
  val onDrawerClosed: EventProp[Event] = new EventProp("MDCDrawer:closed")
  val onDrawerOpen: EventProp[Event]   = new EventProp("MDCDrawer:opened")

  val drawerContentVar: Var[Map[String, HtmlElement]] = Var(
    Map[String, HtmlElement](
      "drawerList" -> new NavigationDrawerList(itemsList)
    )
  )

  val appBarEventsObserver = Observer[AppBarEvents] {
    case PrimaryButtonClicked(buttonType) =>
      if (buttonType == MenuButton) {
        mdcComponent
          .tryNow()
          .map(_ match {
            case Some(d) => {
              d.open = !d.open
            }
            case None => {}
          })
      }
    case ActionButtonClicked(actionButton) => {}
  }

  def contentElement: HtmlElement = div(
    cls := "mdc-drawer__content",
    children <-- drawerContentVar
      .signal
      .map(_.toSeq)
      .split(_._1)((_, inp, sign) => inp._2)
  )

  protected lazy val rootElement: HtmlElement = aside(
    cls := "mdc-drawer mdc-drawer--modal",
    contentElement,
    materialList.clickEvents --> { _ => fromFoundation(_.open = false) },
    onDrawerClosed --> { _ =>
      dom
        .document
        .querySelector("input, button, a")
        .asInstanceOf[dom.html.Element]
        .focus()
    },
    onMountCallback { ctx =>
      mdcComponent.set(
        Some(MDCDrawer.attachTo(dom.document.querySelector(".mdc-drawer")))
      )
    }
  )

}
