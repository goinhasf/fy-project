package components.navigation.drawer

import components.list.MaterialList
import com.raquo.laminar.api.L._
import components.id.ComponentID
import components.MDCList

class NavigationDrawerList(initialItems: Seq[NavigationDrawerItem])
    extends MaterialList[NavigationDrawerItem](initialItems) {
  override protected lazy val rootElement: HtmlElement = {
    nav(
      idAttr := id.toString(),
      cls := "mdc-list",
      children <-- listItems
        .signal
        .split(_.id.toString())((_, item, _) => item.render()),
      onMountCallback(ctx => mdcComponent.set(Some(new MDCList(ctx.thisNode.ref)))),
      onMountCallback(ctx => listItems.now().headOption.map(_.amend(tabIndex := "0")))
    )
  }

  def clickEvents = EventStream.mergeSeq(listItems.now().map(_.events(onClick)))
}
