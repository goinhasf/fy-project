package components.navigation.drawer

import components.list.item.MaterialListItem
import com.raquo.laminar.api.L._
import components.ripple.Ripple

trait NavigationDrawerItem extends MaterialListItem {

  override lazy protected val rootElement: HtmlElement = a(
    cls := "mdc-list-item",
    padding := "0.5rem",
    idAttr := id.toString(),
    cls := id.componentName,
    Ripple(id),
    beforeListElement,
    textElement,
    afterListElement
  )
}
