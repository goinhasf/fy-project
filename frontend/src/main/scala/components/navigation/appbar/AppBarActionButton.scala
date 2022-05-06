package components.navigation.appbar

import components.button.label.ButtonLabel
import components.button.label.IconButtonLabel
import components.MaterialComponent
import components.MDCComponent
import com.raquo.laminar.api.L._

class AppBarActionButton(icon: String, ariaLabel: String = "") extends MaterialComponent[MDCComponent] {
      
    protected val rootElement: HtmlElement = button(
    cls := "material-icons mdc-top-app-bar__action-item mdc-icon-button",
    aria.label := ariaLabel,
    icon,
  )
}
