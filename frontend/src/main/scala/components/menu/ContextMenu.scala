package components.menu

import components.MaterialComponent
import components.MDCMenu
import com.raquo.laminar.api.L._
import components.MDCComponent
import components.id.Identifiable
import components.id.ComponentID
import components.Material
import com.raquo.airstream.eventbus.EventBusStream

class ContextMenu(items: Seq[ContextMenuItem])
    extends MaterialComponent[MDCMenu]
    with Identifiable {
  val id: ComponentID = ComponentID("mdc-menu")

  protected val rootElement: HtmlElement = {
    div(
      cls := "mdc-menu mdc-menu-surface",
      ul(
        cls := "mdc-list",
        role := "menu",
        aria.hidden := true,
        aria.orientation := "vertical",
        tabIndex := "-1",
        items
      ),
      onMountCallback(ctx =>
        mdcComponent.set(Some(new MDCMenu(ctx.thisNode.ref)))
      )
    )
  }
}

case class MenuAnchoredComponent(
    element: HtmlElement,
    contextMenu: ContextMenu
) extends MaterialComponent[MDCComponent]
    with Identifiable {

  val id: ComponentID = ComponentID("mdc-anchored-menu")
  protected val rootElement: HtmlElement = div(
    idAttr := id.toString(),
    cls := "mdc-menu-surface--anchor",
    element.amend(onClick --> { _ =>
      contextMenu
        .mdcComponent
        .now()
        .map(menu => {  
          menu.open = !menu.open
        })
    }),
    contextMenu
  )
}

class ContextMenuItem(text: String) extends MaterialComponent[MDCComponent] {

  val eventBus = new EventBus[Unit]

  protected val rootElement = li(
    cls := "mdc-list-item",
    role := "menuitem",
    span(cls := "mdc-list-item__ripple"),
    span(cls := "mdc-list-item__text", text)
  )
}
