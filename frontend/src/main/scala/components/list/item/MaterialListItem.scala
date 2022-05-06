package components.list.item

import com.raquo.laminar.api.L._
import components.MaterialComponent
import components.MDCComponent
import components.ripple.Ripple
import components.id.Identifiable
import components.id.ComponentID
import components.button.MaterialButton
import components.button.label.IconButtonLabel
import components.MDCButton
import components.MaterialComponent.MaterialComponentAttributes
import components.list.item.builders.{
  AfterListItemElement,
  BeforeListItemElement,
  ListItemTextElement
}

abstract class MaterialListItem
    extends MaterialComponent[MDCComponent]
    with Identifiable
    with ListItemTextElement
    with BeforeListItemElement
    with AfterListItemElement {
  override val id: ComponentID = ComponentID("mdc-list-item")

  override protected lazy val rootElement: HtmlElement = {
    li(
      idAttr := id.toString(),
      cls := id.componentName,
      Ripple(id),
      beforeListElement,
      textElement,
      afterListElement
    )
  }
}
object MaterialListItem {
  val baseStyle = "mdc-list-item"
}
