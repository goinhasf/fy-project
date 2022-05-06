package components.navigation.appbar

import org.scalajs.dom
import com.raquo.laminar.api.L._
import com.raquo.laminar.keys.ReactiveHtmlAttr
import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.domtypes.generic.builders.Tag
import com.raquo.laminar.nodes.ReactiveElement
import components.navigation.drawer.{NavigationDrawerEvents, ToggleState}
import components.MaterialComponent
import components.MDCTopAppBar
import components.menu.MenuAnchoredComponent
import components.menu.ContextMenu
import components.menu.ContextMenuItem

case class TopAppBar(
    initialTitle: String,
    initialActionButtons: Seq[AppBarActionButton] = Seq(
      new AppBarActionButton("more_vert", "More") {
        override def render(): HtmlElement = new MenuAnchoredComponent(
          super.render(),
          new ContextMenu(Seq(new ContextMenuItem("Settings")))
        )
      }
    )
) extends MaterialComponent[MDCTopAppBar] {

  val id                                          = "top-app-bar"
  val title: Var[String]                          = Var(initialTitle)
  val actionButtons: Var[Seq[AppBarActionButton]] = Var(initialActionButtons)
  val primaryButton                               = Var("menu")
  val primaryButtonClickEvent                     = Var(PrimaryButtonClicked(MenuButton))
  val eventBus: EventBus[AppBarEvents]            = new EventBus[AppBarEvents]

  override protected lazy val rootElement: HtmlElement = header(
    idAttr := id,
    cls := "mdc-top-app-bar",
    div(
      cls := "mdc-top-app-bar__row",
      section(
        cls := "mdc-top-app-bar__section mdc-top-app-bar__section--align-start",
        button(
          cls := "material-icons mdc-top-app-bar__navigation-icon mdc-icon-button",
          ariaLabel := "Open navigation drawer",
          child.text <-- primaryButton.signal,
          onClick.mapTo {
            if (primaryButton.now() == TopAppBar.BackButton) {
              PrimaryButtonClicked(BackButton)
            } else {
              PrimaryButtonClicked(MenuButton)
            }
          } --> eventBus
        ),
        ariaLabel := "Open navigation menu",
        span(cls := "mdc-top-app-bar__title", child.text <-- title.signal)
      ),
      section(
        cls := "mdc-top-app-bar__section mdc-top-app-bar__section--align-end",
        role := "toolbar",
        children <-- actionButtons
          .signal
          .map(
            _.map(button =>
              button.editRoot(
                onClick.mapTo(ActionButtonClicked(button)) --> eventBus
              )
            )
          )
      )
    )
  )

  def onBackButtonClicked = eventBus
    .events
    .collect({ case PrimaryButtonClicked(buttonType) => buttonType })
    .filter(_ == BackButton)

  def onMenuButtonClicked = eventBus
    .events
    .collect({ case PrimaryButtonClicked(buttonType) => buttonType })
    .filter(_ == MenuButton)
}

object TopAppBar {
  val MenuButton = "menu"
  val BackButton = "arrow_back"
}
